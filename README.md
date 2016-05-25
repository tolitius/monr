# monr

(rate "drinking beer")

### What does monr like?

monr likes to rate things. It's a less of a "five star" kind of rating and more of "ops/sec".

### How do I get one of those?

#### leiningen
```clojure
[monr "0.1.6"]
```

#### maven
```xml
<dependency>
  <groupId>monr</groupId>
  <artifactId>monr</artifactId>
  <version>0.1.6</version>
</dependency>
```

## How do I use it?

monr gives options:

* monr does it all: just add that (rate "name") where you need it
* Something controls the rate: monr takes a "total count" :update function
* You control the rate: an :inc-count function

### monr does it all: just add that (rate "name") where you need it

A simple way to get started is to just add a (rate "name") to where you need to monitor the rate.
Here are the docs for the rate macro:

```clojure
user=> (doc rate)
   creates a rate monitor and gets evaluated to just "(inc counter)"
   where a "counter" is a thing that monitor is aware of.
   hence can be placed anywhere in the code where rate needs to be monitored:

     (let [bottle (http/get "http://www.beer.com/bottle")]
       (drink bottle)
       (rate "drinking beer"))

   will monitor how fast (how many bottles a second) you can drink
```

Of course none of Clojure libraries would be great if they can't be demoed in REPL:

```clojure
(use 'monr)
```
You can't seriously believe that drinking beer is not as cool as calculating factorials, and not just some factorials but factorials of 42! (or should I say !42):
```clojure
(defn ! [n] (reduce *' (range 1 (inc n))))

(! 42)
1405006117752879898543142606244511569936384000000000N
```
So how many of these per second can we do if we do nothing else (well nothing besides counting "how many of these per second we can do"):
```clojure
(dotimes [_ 100000000] (rate "42 factorial") (! 42))
```
```
INFO:
/-------------------------------------------------------------------------\
|        Name        |           Rate        |            Total           |
|-------------------------------------------------------------------------|
|      42 factorial  |       164,394 ops/sec |                  2,410,786 |
\-------------------------------------------------------------------------/
```
and monr is going to keep going, so we need to "Ctrl + C" it to move along...

### Something controls the rate: monr takes a "total count" :update function

While "rate" is a nice little macro, "crate" is a function that "puts that rate in a crate". Here are the docs for "crate":

```clojure
user=> (doc crate)
  creates a rate monitor

     :interval    how often the rate gets published     :default 5 seconds
     :id          id/name of this rate                  :default (gensym "id:")
     :publish     publisher function                    :default "default-report"
     :update      function to update a total count      :default nil
     :group       whether to add this rate to a group   :default true
```

We'll talk more about each option that it takes, for now let's look at ":update" which takes a "function to update a total count".
A "total count" is kept somewhere else (i.e. DB, coming in as TCP/HTTP request, sitting in a text file, etc..), and :update would take
a function that knows how to get that "total count".

Here is how to create a rate monitor using such a function:

```clojure
(crate :update total-count)
```

where "total-count" is a custom function that is capable of getting that count.

Here is an example of such a function that is getting a "total count" of nanoseconds since the Unix epoch. Here we're also 
going to give it an id:

```clojure
(use 'monr)

(crate :update #(System/nanoTime) :id "nano orchestra")
```
```
INFO:
/-------------------------------------------------------------------------\
|        Name        |           Rate        |            Total           |
|-------------------------------------------------------------------------|
|    nano orchestra  | 1,000,080,200 ops/sec |  1,375,367,673,133,243,000 |
\-------------------------------------------------------------------------/
```
It is an intersting rate to observe, since we would expect (System/nanoTime) rate to be exactly 1,000,000,000 ops a second, 
but depending on what else is running at that same time that distracts CPU, on the call to (System/nano) itself, 
on what JIT is doing, how JVM is feeling, etc.. this rate is going to flactuate a bit, but will stay very close to a billion.

(System/nanoTime) is of course not a very useful rate, but it demostrates how an external "total count" function can be used.

Since "crate" does not block, if it is executed in the REPL, in order to stop it:

```
(stop-all)
```

Another way to stop it would be to do it by its monitor that gets returned from a call to crate, but we'll discuss it a bit later.

### You control the rate: an :inc-count function

"crate", that we discussed briefly above, also returns things. Here is a map of these things:

```clojure
{
 :inc-count        ;; a function that, when called with no args, increments the monitor's counter
 :latest-rate      ;; the latest rate in a form of {:rate 2.75073905437025984E17, :current 1375369527185130000}
 :mon              ;; a monitor instance (i.e. ScheduledFutureTask)
 :id               ;; an ID of a monitor (i.e. "nano orchestra")
}
```
Hence if you want to controll the rate yourself, you just need an ":inc-count" function that can be called every time an operation
is made. The reason it can be handy to do that vs. just a "rate" macro, is that this allows to have a handle to the actual monitor
and other things such as the latest rate.

Here is an example of calculating that same "factorial 42", but now with using "inc-count" function:

```clojure
(defn ! [n] (reduce *' (range 1 (inc n))))

(let [{:keys [inc-count]} (crate)] 
  (loop [] 
    (inc-count) 
    (! 42)
    (recur)))
```
```
INFO:
/-------------------------------------------------------------------------\
|        Name        |           Rate        |            Total           |
|-------------------------------------------------------------------------|
|           id:2179  |       164,584 ops/sec |                  2,174,742 |
\-------------------------------------------------------------------------/
```

Notice we did not give "crate" an id and just called (crate), so it did generate an id for itself: "id:2179"

## Useful Perks

monr has a couple of useful perks to share

### Grouping Rates

In case more than one rate is needed, monr will group them for you in a single report. It is a default behavior. 
In case a certain rate does not need to be grouped, it can be started with ":group false".

Here is an example of grouping two rates together. The gist is: "in order to group them, just start them":

```clojure
(use 'monr)

(crate :update #(System/nanoTime) :id "nano orchestra")
(crate :update #(System/currentTimeMillis) :id "millis orchestra")
```
```
INFO:
/-------------------------------------------------------------------------\
|        Name        |           Rate        |            Total           |
|-------------------------------------------------------------------------|
|    nano orchestra  | 1,000,052,400 ops/sec |  1,375,371,072,913,170,000 |
|  millis orchestra  |         1,000 ops/sec |          1,375,371,072,911 |
\-------------------------------------------------------------------------/
```

These can be started in different places at different times, they'll still be grouped together.
One sample useful stats could look like:

```
INFO monr.group:
/-------------------------------------------------------------------------\
|        Name        |           Rate        |            Total           |
|-------------------------------------------------------------------------|
|      engine calls  |       472,469 ops/sec |            112,188,913,708 |
|        sent to DB  |       460,403 ops/sec |            109,386,034,899 |
|     http requests  |        32,724 ops/sec |                  4,024,910 |
|     cache reloads  |             2 ops/sec |                      2,427 |
\-------------------------------------------------------------------------/
```

### Custom Publisher

While looking at the rates in logs is cool, monr can "consider" a custom publishing function, 
that could do anything with rates (i.e. send them to Redis, Datomic, Websocket, etc..). Here is how:

```clojure
(crate :publish publisher)
```

where "publisher" is a function that on a specified or default interval will be given a map {:id ... :rate ... :current ...}
and can do anything with these stats. Here is an example of a custom publisher:

```clojure
(use 'monr 'clojure.tools.logging)   ;; will use tools.logging to demo it

(defn pub [{:keys [id rate current]}] 
  (info (format "[ID: %s, RATE: %,12d, TOTAL: %,13d]"
                id (long rate) current)))

(crate :update #(System/nanoTime) :id "nano orchestra" :group false :publish pub)
```
```
;; and get these pumping every "interval"
INFO: [ID: nano orchestra, RATE: 1,000,008,000, TOTAL: 1,375,372,668,926,272,000]
```

One thing to notice: ":group false". Thinking is that if there is a custom publisher, this particular monitor does not need to
join the group. This can be changed, of course, in the future.

### Reading rates on demand

monr can be asked to provide the rates on demand. It can be useful if there is an (external, but not necessarily) 
service/system/function that needs to read rates at its own leisure. 

Latest rates could be read by (read-rates monitor), where "monitor" is a value returned from "crate". For example:

```clojure
(use 'monr)

(def mon (crate :update #(System/nanoTime) :id "nano orchestra"))

(read-rate mon)
{:id "nano orchestra", :rate 1.0001226E9, :current 1375373474692957000}
```

Each subsequent call to "read-rates" will always get the latest rates from this monitor.

### Bench a function

monr likes to rate things, and to monitor these rates, but it is not a _benching_ library. [criterium](https://github.com/hugoduncan/criterium) is.

However there is a little "bench" macro up monr's sleeve that can be used to quickly get a feel for "how fast" a function is:

```clojure
(use 'monr.bench)

(defn ! [n] (reduce *' (range 1 (inc n))))

(bench (! 42))
```
```
INFO:
/-------------------------------------------------------------------------\
|        Name        |           Rate        |            Total           |
|-------------------------------------------------------------------------|
|           id:1515  |       164,998 ops/sec |                  6,718,105 |
\-------------------------------------------------------------------------/
```

## License

Copyright © 2013 tolitius

Distributed under the Eclipse Public License, the same as Clojure.
