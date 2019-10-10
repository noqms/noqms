# noqms
NoQMS - No Queue Microservices - Java Framework

![alt text](architecture.svg)

Microservices without a centralized queue is a perfectly viable architecture given there is 
an efficient way for the microservices to discover each other. UDP multicast is a great solution for
this - unfortunately most cloud providers do not support UDP multicast. This framework includes a pluggable
Service Finder which allows users to create their own discovery mechanism and class, replacing 
the built in UDP multicast discovery mechanism if needed.

I coined the term NoQMS - No Queue Microservices - to describe microservices with no dependency on
a centralized queue.

This Java implementation of the NoQMS architecture shown above is lean and mean - the central
processor itself is a single thread with no dependencies on blocking processes. 

Your microservice code runs in as many configurable threads as you want.
This allows you to adjust to better take advantage of the (virtual) environment's resources such as CPU and memory. 
Outside of the single (potentially multi-threaded) microservice instance, the framework supports discovering and
utilizing all microservice instances (running, ideally, on virtual environments of their own) 
that are making their presence and availability known. This yields scalability and also better reliability in the event 
of failure as you would expect. A noqms microservice can also run within a container orchestration system
and benefit from all the goodies that brings just like traditional microservices. 

UDP unicast is an excellent choice for the application level inter microservice messages. Developers need to be wiser 
about dismissing UDP offhand. Utilized correctly, it scales far beyond TCP for obvious reasons. We must never dismiss 
the actuality of just how reliable UDP can be when there is, in fact, something on the other side expecting 
the data and processing it in a timely fashion. The downside of UDP includes single packet limits of under 64K. So write your 
microservices accordingly, thinking carefully about not turning it into a <i>macro</i>service before applying 
workarounds (paging, application level packet reassembly, etc) for that limit. Additionally, in the event of
transmission failure, we know that we must program for failure anyway in order to have a robust system. Timeouts 
are an integral part of the noqms framework, covered next.

Timeouts are first class citizens in this architecture. With each microservice the application developer specifies the
typicalMillis and the timeoutMillis for that microservices. The framework handles the rest - notifying a requester when
a response has timed out, for example. Waiting for a response will not take longer than the receiving side's reported
timeout, and the response information indicates whether a timeout occured. The microservice can handle the timeout 
if the way to handle it at its level is clear, or simply pass it up the chain, sending an application defined status code back 
to the microservice that requested the data from <i>it</i>.  Programming for and explicity handling more failure cases - which includes timeouts - makes for a more robust system. The alternative can sometimes mean long weekends for IT and fixing or recovering data
because of an unforeseen slowdown somewhere in the system that caused cascading failures. 

One benefit of not having a centralized queue - and of the framework being capable of instantiation any number of
microservices locally and within the same process (not the intended production scenario) - is that the development 
and debugging phase is now nearly back to "not harder" than traditional architectures. The examples and tests demonstrate how 
to run one or more microservices from an external process. 

[Examples](https://github.com/noqms/noqms-examples) and [Tests](https://github.com/noqms/noqms-tests) for this framework
reside in sibling projects.
