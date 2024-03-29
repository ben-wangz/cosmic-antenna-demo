# cosmic-antenna-demo

### building steps

1. install podman

    ```shell
    systemctl stop firewalld && systemctl disable firewalld
    sudo dnf install -y podman
    podman run -d -P m.daocloud.io/docker.io/library/nginx
    ```

2. install kind
    ```shell
   # install kind and kubectl
    mkdir -p $HOME/bin \
    && export PATH="$HOME/bin:$PATH" \
    && curl -o kind -L https://resource-ops.lab.zjvis.net:32443/binary/kind/v0.20.0/kind-linux-amd64 \
    && chmod u+x kind && mv kind $HOME/bin \
    && curl -o kubectl -L https://resource-ops.lab.zjvis.net:32443/binary/kubectl/v1.21.2/bin/linux/amd64/kubectl \
    && chmod u+x kubectl && mv kubectl $HOME/bin
    ```

    ```shell
    # create a cluster using podman
    curl -o kind.cluster.yaml -L https://gist.githubusercontent.com/AaronYang2333/71335475279c8f9214e58e3556b0e84f/raw/c7c3886444bf6824b87be3d2e700ed77844a7466/kind-cluster.yaml \
    && export KIND_EXPERIMENTAL_PROVIDER=podman \
    && kind create cluster --name cs-cluster --image m.daocloud.io/docker.io/kindest/node:v1.27.3 --config=./kind.cluster.yaml
    # need to modify ~/.kube/config ---BUG
    ```
   
   ```shell
   # download slow image
   DOCKER_IMAGE_PATH=/root/docker-images && mkdir -p $DOCKER_IMAGE_PATH
   BASE_URL="https://resource-ops-dev.lab.zjvis.net:32443/docker-images"
   for IMAGE in "quay.io_argoproj_argocd_v2.9.3.dim" \
       "ghcr.io_dexidp_dex_v2.37.0.dim" \
       "docker.io_library_redis_7.0.11-alpine.dim" \
       "docker.io_library_flink_1.17.dim"
   do
       IMAGE_FILE=$DOCKER_IMAGE_PATH/$IMAGE
       if [ ! -f $IMAGE_FILE ]; then
          TMP_FILE=$IMAGE_FILE.tmp \
          && curl -o "$TMP_FILE" -L "$BASE_URL/$IMAGE" \
          && mv $TMP_FILE $IMAGE_FILE
       fi
       kind -n cs-cluster load image-archive $IMAGE_FILE
   done
   ```

    ```shell
    # install cert-manger ingress argo-cd
    kubectl create namespace argocd \
    && kubectl apply -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml \
    && kubectl apply -n argocd -f https://gist.githubusercontent.com/AaronYang2333/10091b7dfeafb0c373aa38659ad9925b/raw/653421b67922e63c0df00b7f2b68886b2026a991/argo-cd.server.yaml
    # download argo-cd cli
    curl -sSL -o $HOME/bin/argocd https://hub.yzuu.cf/argoproj/argo-cd/releases/download/v2.8.4/argocd-linux-amd64 \
    && chmod u+x $HOME/bin/argocd
    # github available mirrors -> hub.njuu.cf/   hub.yzuu.cf/  hub.nuaa.cf/   hub.fgit.ml/
    ```
    
    ```shell
    # get initial argo-cd password
    kubectl -n argocd get secret argocd-initial-admin-secret -o jsonpath="{.data.password}" | base64 -d
    ```
   
    ```shell
    # login argo-cd
    argocd login --insecure --username admin localhost:1443
    # open https://ip:1443 in browser
    ```   
    
3. install essential app on argocd
    ```shell
    # install cert manger    
    curl -LO https://gist.githubusercontent.com/AaronYang2333/ec9446c1af9c0c0f7807dd84f81a7c0e/raw/0800c914c66f3e227732b5db8fcde5dc7f650804/cert-manager.yaml \
    && kubectl -n argocd apply -f cert-manager.yaml \
    && argocd app sync argocd/cert-manager
    
    # install ingress
    curl -LO https://gist.githubusercontent.com/AaronYang2333/76afa21b7c07f0d4006b419c0c33a424/raw/a5ac2bc4b1e6b874eeea70fdccfa2d6440ec960a/ingress-nginx.yaml \
    && kubectl -n argocd apply -f ingress-nginx.yaml \
    && argocd app sync argocd/ingress-nginx
   
    # install flink-kubernetes-operator
    curl -LO https://gist.githubusercontent.com/AaronYang2333/090b92143f5a212c5909fc87ccb84833/raw/0b51e2ff318ffe59963e7b283bb6b13ae16a12f5/flink-operator.yaml \
    && kubectl -n argocd apply -f flink-operator.yaml \
    && argocd app sync argocd/flink-operator
    ```

4. install git

    ```shell
    sudo dnf install -y git \
    && rm -rf $HOME/cosmic-antenna-demo \
    && mkdir $HOME/cosmic-antenna-demo \
    && git clone --branch pv_pvc_template https://github.com/AaronYang2333/cosmic-antenna-demo.git $HOME/cosmic-antenna-demo
    ```

5. prepare image
    ```shell
   # build application images 
   # cd into  $HOME/cosmic-antenna-demo
    sudo dnf install -y java-11-openjdk.x86_64 \
    && $HOME/cosmic-antenna-demo/gradlew :s3sync:buildImage \
    && $HOME/cosmic-antenna-demo/gradlew :fpga-mock:buildImage
    ```

    ```shell
    # save and load into cluster
    VERSION="1.0.3"
    podman save --quiet -o $DOCKER_IMAGE_PATH/fpga-mock_$VERSION.dim localhost/fpga-mock:$VERSION \
    && kind -n cs-cluster load image-archive $DOCKER_IMAGE_PATH/fpga-mock_$VERSION.dim
    podman save --quiet -o $DOCKER_IMAGE_PATH/s3sync_$VERSION.dim localhost/s3sync:$VERSION \
    && kind -n cs-cluster load image-archive $DOCKER_IMAGE_PATH/s3sync_$VERSION.dim
    ```
   
   ```shell
   # add services and endpoints authority
   kubectl -n flink edit role/flink -o yaml
   ```

6. prepare k8s resources [pv, pvc, sts]
    ```shell
    cp -rf $HOME/cosmic-antenna-demo/flink/*.yaml /tmp \
    && podman exec -d cs-cluster-control-plane mkdir -p /mnt/flink-job
    # create persist volume
    kubectl -n flink create -f /tmp/pv.template.yaml
    # create pv claim
    kubectl -n flink create -f /tmp/pvc.template.yaml
    # start up flink application
    kubectl -n flink create -f /tmp/job.template.yaml
    # start up ingress
    kubectl -n flink create -f /tmp/ingress.forward.yaml
   ```
   
   ```shell
    # start up fpga UDP client, sending data 
    cp $HOME/cosmic-antenna-demo/fpga-mock/client.template.yaml /tmp \
    && kubectl -n flink create -f /tmp/client.template.yaml
    ```

7. check dashboard in browser

   ```shell
   # go to http://job-template-example.flink.lab.zjvis.net
   ```


# Reference
1. https://github.com/ben-wangz/blog/tree/main/docs/content/6.kubernetes/7.installation/ha-cluster
2. xxx