# Flight attendant

This is a quick prototype playing with the Java Flight Recorder (JFR) [event streaming api](https://openjdk.java.net/jeps/349) in Java 14. The project can be used as a java agent to receive events and send them to Elastic Search. 

## Why?

The JDK mission control console is great for local projects are if you can reach the correct ports. This isn't always feasible. This is an example of how this data could be exported to a centrally managed search engine. 

## Try it out

I wanted to use the official ElasticSearch client jar for convenience. Clone the repository and build a fat jar to use as a java agent :

```sh
./gradlew shadowJar
```

Start your Java application and pass it the agent 

```sh
java -javaagent:flight-attendant/build/libs/flight-attendant-all.jar -jar my-app.jar
```

