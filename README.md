# noqms
NoQMS - No Queue Microservices - Java Framework

![alt text](architecture.svg)

Microservices without a centralized queue is a perfectly viable architecture given there is 
an efficient way for the microservices to discover each other. UDP multicast is a perfect solution for
this - unfortunately most cloud providers do not support UDP multicast. This framework includes a pluggable
Service Finder mechanism which allows users to create their own discovery mechanism and class, replacing 
the built in UDP multicast discovery mechanism if needed.

The Java framework here is built to be lean and mean - the central processor itself is a single thread with no 
dependencies on blocking processes. 

You microservice code itself runs in as many configurable threads as you want.
This allows you to adjust to better take advantage of the (virtual) environment's resources such as CPU and memory. 
Outside of the single (potentially multi-threaded) microservice instance, the framework supports running as many 
instances of a unique microservice as you like (ideally on other virtual environments) for overall scalability and 
to achieve better reliability in the event of failure. Nothing prevents a noqms microservice instance from running 
within configurable Kubernetes pods, with all the goodies that brings, for example, and just like traditional 
microservices. There should always be more microservice instances of any unique microservice than what is 
required for full load.  

UDP unicast is an excellent choice for the inter microservice messages. Developers need to be wiser about dismissing
UDP offhand. Utilized correctly, it scales far beyond TCP for obvious reasons. We must never dismiss 
the actuality of just how reliable UDP can be when there is, in fact, something on the other side expecting 
the data and processing it in a timely fashion. The downside of UDP includes single packet limits of under 64K. So write your 
microservices accordingly, thinking carefully about not turning it into a macroservice before applying 
workarounds (paging, application level packet reassembly, etc) for that 64K limit. Additionally, in the event of
transmission failure, we know that we must program for failure anyway in order to have a robust system. Timeouts 
are an integral part of the noqms framework, covered next.

Timeouts are first class citizens in this architecture. With each microservice the application developer specifies the
typicalMillis and the timeoutMillis for that microservices. The framework handles the rest - notifying a requester when
a response has timed out, for example. Waiting for a response will not take longer than the receiving side's reported
timeout, and the response information indicates whether a timeout occured. The microservice can handle it there at that
level if the logic is clear, or simply pass it back to the microservice that requested the data from <i>it</i>. 
Programming for and explicity handling more failure cases - which includes timeouts - makes for 
a more robust system. The alternative can sometimes mean long weekends for IT and recovering lost or corrupt data
because of an unforeseen slowdown somewhere in the system that caused cascading failures. 

[Examples](https://github.com/noqms/noqms-examples) and [Tests](https://github.com/noqms/noqms-tests) for this framework
reside in sibling projects.
