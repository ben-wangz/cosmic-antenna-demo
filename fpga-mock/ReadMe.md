### Run argument

1. #### host
    - argument: -Dhost
    - default value: "127.0.0.1"
2. #### port
    - argument: -Dport
    - default value: "11111"
    - range: [0, 65536]
3. #### iterator
    - argument: -Dhost
    - default value: "1000"
    - range: [-1, 2^64]
    - desc: -1 means won't stop.
4. #### interval
    - argument: -Dhost
    - default value: "3000"
    - desc: <b>how often send data to server</b>, default value 3000 means 3 seconds.
5. #### timeSampleSize
   - argument: -DtSize
   - default value: "2048"
   - desc: 


### Example
```shell

java -Dport=22222 -Dhost=192.168.1.101 -Diter=11 -Dinterval=1000 -DtSize=4096 -jar fpga-mock-all.jar
```