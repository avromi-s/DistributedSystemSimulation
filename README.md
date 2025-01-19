# Distributed System Simulation

This project simulates a basic representation of a distributed system comprising a __client__, __slave__, and __master__ application.

The client application requests the completion of jobs, the slave application executes jobs, and the master application manages all the slaves, clients, and jobs.

***

The flow of a job through the system is as follows:
- A client sends a job request to the master
- The master assigns the job request to the slave that will finish it the soonest, considering the job type and the current load on all slaves (load balancing)
- The slave 'executes' the job (this is simulated by sleeping the slave for a set amount of time; _see note below_)
- On completion, the slave sends back the job result to the master
- The master sends back the job result to the client

_For the simulation, we make use of two job types - type A and B. On startup, each slave application is set to be optimized for only one of the job types. A slave application will execute the job type it is optimized for faster than one it is not optimized for. The master application takes this into account when load balancing for better optimization._

## How to run
-   Download and install the client, slave, and master applications (found in the [releases](https://github.com/avromi-s/DistributedSystemSimulation/releases/); alternatively, you can build it from source)
    -   They can be installed on the same or separate computers
-   Startup one instance of the master application
-   Startup one or more instances of the client application
-   Startup one or more instances of the slave application
-   On the client and slave applications, enter the IP address where the master application is running:
    -   If you are running both applications on the same computer, use `127.0.0.1`
    -   If the applications are running on different networks, port forwarding will need to be configured on the *master* application's network to forward all incoming traffic for port `30000` and `30001` to the local machine that is                 running the master application
-   Connect the slave application(s) to the master application
-   Send job requests to the master from the client application(s)

***

|                |                                    |
|----------------|------------------------------------|
| Master application   | <img width="437" alt="master-application-running" src="https://github.com/user-attachments/assets/826e2a78-14f4-4bd2-80ff-efe0204099ac" />  |
| Slave application  | <img width="438" alt="slave-application-running" src="https://github.com/user-attachments/assets/deec243a-3b4b-4324-9a40-0ab7c8328d41" /> |
| Client application   | <img width="437" alt="client-application-running" src="https://github.com/user-attachments/assets/edbb4cb3-eaa9-48f7-9e3f-1aa025487843" /> |
