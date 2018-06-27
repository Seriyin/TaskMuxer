# Task Scheduler #

A task scheduler heavily reliant on the Spread group
communications toolkit.

# Build #

Run gradle build for client and server.

> gradle build

## Servers ##

### Introduction ###

A simple replicated task scheduler is implemented in muxer-server.

Passive replication is achieved via extensive use of delta messages.

### Tasks ###

Task is the smallest unit of work considered. It must return a
result.

If a Task is Asynchronous it returns a CompletableFuture.

## Usage ##

> muxer-server true

Start the first server as the leader.

> muxer-server

Run a secondary service.

After updating, if a leader dies, one of these secondaries is
chosen automatically as a new leader.

## Clients ##

### Introduction ###

A simple dummy client is provided that simply spams useless tasks
which take a fair amount of time.

### Usage ###

> muxer-client

Runs a dummy client service.

## Extending ##

To make use of this code, devise new Tasks and implement an
identical Client that runs them.