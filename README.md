# Netty Loom Experiment

This repository contains [Project Loom](https://openjdk.java.net/projects/loom/) and [Netty](https://netty.io/) related test code.

## Contents / Goals

I created these examples since I was curious about Project Loom and what it would mean for my Netty based [Vert.x](https://vertx.io/) projects. 

## Netty

In my first test I created a regular netty server which makes use of the `NioEventLoopGroup`. A custom thread factory is used to enable Netty to use VirtualThreads instead of platform threads.

I went further down the rabbit hole since I wanted to check whether it would be possible to use on-demand virtual threads instead of pooling virtual threads. I thus created the `LoomNioEventLoopGroup`. Unfortunatly my knowledge of selector based IO is very limited and I did not yet find a way to change the request handling.

For comparison I also created the `#testPlatformThreads` variant which uses regular platform threads.

## NIO

In order to further experiment with NIO Server handling I created a few additional testcases to check whether there are other potential ways to make use of virtualthreads.

The `SimpleAsyncSocketServerTest` contains a basic NIO server which only uses platform threads. I started from here to learn more about `AsynchronousServerSocketChannel`.

In the `NioServerExampleTest` I use virtual threads to handle previously accepted connections. Actually I'm a bit surprised that this example even works since the selector in the thread is registered after the connection has been accepted.


Next I had the idea to use the `AsynchronousServerSocketChannel` which has the benefit that it can accept callbacks for request handling. The `#testServerWithFork` test forks a dedicated virtual thread in the accept handler for each connection. The `#testServerWithGroup` uses the option to setup a `AsynchronousChannelGroup` to handle threading internally. This is similar to Netty as it works with a dedicated thread pool.

## Loom

During my testing I noticed that Netty would not be able to process requests when I configured a thread pool size of `500+`. I created the `LoomThreadTest` to replicate this issue and found that the JVM would not run / start a virtual thread of `400+` previous threads were calling `new Selector().select()`. I'm not yet sure why this is the case.


## Used Versions:
* Netty Version: [4.1.70.Final-SNAPSHOT@77b7c1a56dcd4fc964137f21caf321a1ca19c0ed](https://github.com/netty/netty/tree/77b7c1a56dcd4fc964137f21caf321a1ca19c0ed)
* Loom JDK Version - build 18-loom+2-74 - 2022-03-15@860ad0abc98c



