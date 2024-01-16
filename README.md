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
    # create a cluster
    curl -o kind.cluster.yaml -L https://gist.githubusercontent.com/AaronYang2333/71335475279c8f9214e58e3556b0e84f/raw/c7c3886444bf6824b87be3d2e700ed77844a7466/kind-cluster.yaml \
    && kind create cluster --name cs-cluster --image m.daocloud.io/docker.io/kindest/node:v1.27.3 --config=./kind.cluster.yaml
    ```
   
   ```shell
   # download slow image
   DOCKER_IMAGE_PATH=/root/docker-images && mkdir -p $DOCKER_IMAGE_PATH
   BASE_URL="https://resource-ops-dev.lab.zjvis.net:32443/docker-images"
   for IMAGE in "quay.io_argoproj_argocd_v2.9.3.dim" \
                "ghcr.io_dexidp_dex_v2.37.0.dim" \
                "redis_7.0.11-alpine.dim"
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
    curl -LO https://raw.githubusercontent.com/ben-wangz/blog/main/docs/public/kubernetes/argocd/cert-manager/cert-manager.yaml
    kubectl -n argocd apply -f cert-manager.yaml
    argocd app sync argocd/cert-manager
    
    # install ingress
    curl -LO https://raw.githubusercontent.com/ben-wangz/blog/main/docs/public/kubernetes/argocd/ingress/ingress-nginx.yaml
    kubectl -n argocd apply -f ingress-nginx.yaml
    argocd app sync argocd/ingress-nginx
   
    # install flink-kubernetes-operator
    curl -LO https://raw.githubusercontent.com/ben-wangz/blog/main/docs/public/kubernetes/argocd/flink/flink-operator.yaml
    kubectl -n argocd apply -f flink-operator.yaml
    argocd app sync argocd/flink-operator
    ```

4. install git

    ```shell
    sudo dnf install -y git \
    && rm -rf $HOME/cosmic-antenna-demo \
    && mkdir $HOME/cosmic-antenna-demo \
    && git clone --branch pv_pvc_template https://github.com/AaronYang2333/cosmic-antenna-demo.git $HOME/cosmic-antenna-demo
    # switch to specific branch
    ```

5. prepare image
    ```shell
   # build application images [slow 13 mins]
    sudo dnf install -y java-11-openjdk.x86_64 \
    && $HOME/cosmic-antenna-demo/gradlew :s3sync:buildImage \
    && $HOME/cosmic-antenna-demo/gradelw :fpga-mock:buildImage
    # save and load in different node
    # podman save --quiet -o fpga-mock_1.0.3.dim localhost/fpga-mock:1.0.3
    # ctr -n k8s.io image import --base-name localhost/fpga-mock:1.0.3 /tmp/fpga-mock_1.0.3.dim
    ```

6. prepare k8s resources [pv, pvc, statefulSet]
    ```shell
    # copy (asdasda)[./flink/pv.template.yaml]
    cp $HOME/cosmic-antenna-demo/flink/*.yaml /tmp \
    && mkdir /mnt/flink-job
    #
    kubectl -n flink create -f /tmp/pv.template.yaml /tmp/pvc.template.yaml
    #
    kubectl -n flink create -f /tmp/job.template.yaml
    # 
    kubectl -n flink create -f /tmp/ingress.forward.yaml
    # 
    cp $HOME/cosmic-antenna-demo/fpga-mock/client.template.yaml /tmp
    kubectl -n flink create -f /tmp/client.template.yaml
    ```

7. check dashboard in browser

   ```shell
   # go to https://job-template-example.flink.lab.zjvis.net
   ```


# Reference
1. https://github.com/ben-wangz/blog/tree/main/docs/content/6.kubernetes/7.installation/ha-cluster
2. xxx