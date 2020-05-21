# noqms
NoQMS - No Queue Microservices - Java Framework

![alt text](architecture.svg)

I coined the term NoQMS - No Queue Microservices - to describe microservices with no dependency on
a centralized queue.

Microservices without a centralized queue is a perfectly viable architecture given there is 
an efficient way for the microservices to discover each other. UDP multicast is a great solution for
this. UDP multicast is supported by Linode and Digital Ocean, for example, but not by other major
cloud providers. This framework includes a pluggable Service Finder which allows developers 
to create their own discovery mechanism and class, replacing the built in UDP multicast service finder if 
needed. 

This Java implementation of the NoQMS architecture shown above is lean and mean - the central
processor itself is a single thread with no dependencies on blocking processes. 

Your microservice code runs in as many configurable threads as you want.
This allows you to adjust to better take advantage of the environment's resources such as CPU and memory. 
Outside of the single potentially multi-threaded microservice instance, the framework supports discovering and
utilizing all microservice instances that are making their presence and availability known. 
This yields scalability and also better reliability in the event 
of failure as you would expect. A NoQMS microservice can also run within a container orchestration system
and benefit from all the goodies that brings just like traditional microservices. 

UDP is an excellent choice for microservice messages.
Utilized correctly, it scales far beyond TCP for obvious reasons. UDP is also very reliable
when the receiving end is processing the data in a timely fashion. Message delivery failure detection 
(naturally present in a request/response architecture) allows the appropriate action to be taken when a 
communication error or timeout occurs, a requirement also found in any well written TCP reliant application.
It all comes down to architecting and balancing for fewer failures, then handling failure case which will occur 
regardless of what communication tech is utilized. One downside of UDP includes 
single packet limits of under 64K. So write your microservices accordingly, thinking carefully about not turning it 
into a <i>macro</i>service before applying workarounds for that limit. 

Timeouts are first class citizens in this architecture. With each microservice the application developer specifies the
typicalMillis and the timeoutMillis for that microservices. The framework handles the rest - notifying a requester when
a response has timed out, for example. Waiting for a response will not take longer than the receiving side's reported
timeout, and the response information indicates whether a timeout occured. The microservice can handle the timeout 
if the way to handle it at its level is clear, or simply pass it up the chain, sending an application defined status code back 
to the microservice that requested the data from <i>it</i>.  Programming for and explicity handling more failure 
cases - which includes timeouts - makes for a more robust system. 

One benefit of not having a centralized queue - and of the framework being capable of instantiation any number of
microservices locally and within the same process (not the intended production scenario) - is that the development 
and debugging phase is now nearly back to "not harder" than traditional architectures. 

To run noqms:

* Install Java 12 or greater
* Put all of the noqms release files and dependencies in a directory as well as your microservice jar
* cd to that directory
* java -server -cp **xCPx** com.noqms.Runner **key/value arguments**

**xCPx** is *;. for Windows and *:. for Linux

**key/value arguments** are documented in the [Runner](https://github.com/noqms/noqms/blob/master/src/com/noqms/Runner.java)
