# Distance-Vector-Routing-Protocols

# Overview 
This project implements a simplified version of the Distance Vector Routing Protocol using UDP
for communication. The protocol operates on four servers (acting as routers). Each server 
maintains a routing table and can output its forwarding table, handle link changes, and send 
routing updates periodically or upon user request. 

# Features
• Implements a simplified Distance Vector Routing Algorithm 

• Communicates exclusively using UDP sockets

• Supports **periodic routing updates** and user-triggered updates 

• Maintains a routing tables with: 

  • Costs to neighbors. 

  • Propagation of updates upon topology changes.

• Handles:

• **Link cost updates.**

• **Server crashes and reconnection**

• **Server additions and removal**

• **Commands** for interacting with the server dynamically during execution






# Authors
• Alejandro Urbano 

• Emmanuel Gonzalez

• Jonathan Garcia 
