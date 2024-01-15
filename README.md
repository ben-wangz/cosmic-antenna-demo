# cosmic-antenna-demo

### building steps

1. install docker

    ```shell
    systemctl stop firewalld && systemctl disable firewalld
    sudo dnf -y install dnf-plugins-core
    sudo dnf config-manager --add-repo https://download.docker.com/linux/fedora/docker-ce.repo
    ```

    ```shell
    sudo dnf -y install docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
    sudo systemctl start docker
    docker run -d -P m.daocloud.io/docker.io/library/nginx
    ```

2. install kind
    ```shell
    mkdir -p $HOME/bin \
    && export PATH="$HOME/bin:$PATH"
    && curl -o kind -L https://resource-ops.lab.zjvis.net:32443/binary/kind/v0.20.1/kind-linux-amd64 \
    && chmod u+x kind && mv kind $HOME/bin \
    && curl -o kubectl -L https://resource-ops.lab.zjvis.net:32443/binary/kubectl/v1.21.2/bin/linux/amd64/kubectl \
    && chmod u+x kubectl && mv kubectl $HOME/bin
    ```

    ```shell
    # create a cluster
    kind create cluster --name cs-cluster --image m.daocloud.io/docker.io/kindest/node:v1.27.3
    ```

    ```shell
    # install cert-manger ingress argocd
    kubectl create namespace argocd
    kubectl apply -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml
    
    # download argocd cli
    # [slow 9 mins]
    curl -sSL -o $HOME/bin/argocd https://mirror.ghproxy.com/https://github.com/argoproj/argo-cd/releases/download/v2.8.4/argocd-linux-amd64 \
      && chmod u+x $HOME/bin/argocd

    # [Local, optional]
    curl -sSL -o $HOME/bin/argocd https://github.com.cnpmjs.org/argoproj/argo-cd/releases/download/v2.8.4/argocd-linux-amd64 \
      && chmod u+x $HOME/bin/argocd
    ```
    
    ```shell
    # get initial argocd password
    kubectl -n argocd get secret argocd-initial-admin-secret -o jsonpath="{.data.password}" | base64 -d
   
    # login argocd
    argocd login --insecure --username admin ip:30443
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
    sudo dnf install -y git
    ```
    ```shell
    cd ~ && git clone https://github.com/ben-wangz/cosmic-antenna-demo.git
    ```

5. install java
    ```shell
    sudo dnf install -y java-11-openjdk.x86_64
    ```

6. prepare image
    ```shell
   # build application images
    ~/cosmic-antenna-demo/gradlew :s3sync:buildImage \
    && ~/cosmic-antenna-demo/gradelw :fpga-mock:buildImage
    #CHECK do we need to scp and crt load
    docker save -o 
    ctr load
    ```

7. prepare k8s resources [pv, pvc, statefulSet]
    ```shell
    # copy (asdasda)[./flink/pv.template.yaml]
    cp ~/cosmic-antenna-demo/flink/*.yaml /tmp
    mkdir /mnt/flink-job
    #
    kubectl -n flink create -f /tmp/pv.template.yaml /tmp/pvc.template.yaml
    #
    kubectl -n flink create -f /tmp/job.template.yaml
    # 
    kubectl -n flink create -f /tmp/ingress.forward.yaml
    # 
    cp ~/cosmic-antenna-demo/fpga-mock/client.template.yaml /tmp
    kubectl -n flink create -f /tmp/client.template.yaml
    ```

8. check dashboard in browser

   ```shell
   # job-template-example.flink.lab.zjvis.net
   ```


# Reference
1. https://github.com/ben-wangz/blog/tree/main/docs/content/6.kubernetes/7.installation/ha-cluster
2. xxx