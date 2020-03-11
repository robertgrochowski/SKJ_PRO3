start java -classpath "out" skj.pro3.transmitter.Transmitter 9000
timeout 2
start java -classpath "out" skj.pro3.agent.Agent 127.0.0.1 9000 127.0.0.1 12000 12001 12002
start java -classpath "out" skj.pro3.user.Recipient -l 12000 12001 12002 -s 10000 10001 10002
start java -classpath "out" skj.pro3.user.Client 127.0.0.1 12000 10000
start java -classpath "out" skj.pro3.user.Client 127.0.0.1 12001 10001
start java -classpath "out" skj.pro3.user.Client 127.0.0.1 12002 10002