# DistributedSystemSimulation

This project simulates a distributed system comprised of a Client, Slave, and Master application. The Client application requests the completion of jobs, the Slave executes jobs, and the Master manages all the job requests, Slaves, and job results.

The flow of a job through the system is as follows:
- A Client sends a job request to the Master
- The Master assigns the job request to the Slave that will finish it the soonest, considering the current load on all Slaves (load balancing)
- The Slave 'executes' the job request (this is simulated by sleeping the Slave for a set amount of time)
- The Slave sends back the job result to the Master
- The Master sends back the job result to the Client

For the simulation, we make use of two job types, namely type A and B. On startup, each Slave application is set to be optimized for only one of the job types. A Slave application will execute the job type it is optimized for faster than one it is not optimized for. The Master takes this into account when load balancing.
