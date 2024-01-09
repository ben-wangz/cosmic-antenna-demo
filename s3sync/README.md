# s3sync

## description

* sync data from s3 to local
* can be used in distributed environment
* usually for the init containers of k8s

## usage

### build image

```shell
./gradlew :s3sync:buildImage
```

### specify parameters with environment variables

### run with container
1. start s3 service with minio
    * ```shell
      podman run --rm \
          --name data-hub-minio \
          -p 9000:9000 \
          -p 9001:9001 \
          -d minio/minio:RELEASE.2022-09-07T22-25-02Z server /data --console-address :9001
      ```
2. start service
   * ```shell
     set ENDPOINT=http://aaaa:bbbbb
     set BUCKET=ccccc
     set OBJECT_KEY=single file name or dir path (end with /)
     set TARGET_PATH=single file name or dir path (end with /)
     set TIMEOUT_MILLISECONDS=60000
     set HOSTNAME=ddddd
    
   ```

3. 
