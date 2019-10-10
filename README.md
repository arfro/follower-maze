## My notes

### General approach to refactor
In my experience general refactors as this one can quickly turn out to be messy and rushing can result in a disaster so my usual approach is to take baby steps such as:
1. Read the code, understand it and add obvious TODOs. Also mark with comments where code is unclear or maybe a class used I'm not familiar with
2. Sketch out the big picture and add/improve models (in this case: add)
3. Create an empty structure skeleton of the project without yet moving code
4. Start from tiny changes like moving vals out to configs to finish up with chunky parts like removing/renaming code/classes
5. Most importantly - run exiting tests and add new ones (out of scope here) as frequently as possible. Perhaps this could be a little controversial but I would even lean to say after each change. Refactors can go very wrong if this step is omitted.

### Choices
- using algebraic data types to model data. In this case it's only product ADTs (as for `EventError`, `Event` or `Config`). Should we want to introduce e.g. message statuses then a sum ADT would also be used
- sticking to modules which expose services which expose functionality. Only mixing in modules with modules to keep the same level of abstraction
- as 3rd party tools are not allowed I am using self types to achieve dependency injection
- using a util object to keep code that does not "belong" to services nor modules (e.g. `MessageConverter`) to keep the scope clean
- to maintain type safety I use case classes and avoid simple objects such as String or Int. If the type is as simple as e.g. Long (UserId) I alias it

### Third party libraries
I use three 3rd party libraries, two of them in tests and one for config (I really hope using this one can be justified - if not, I obviously could have simply used an object)
- TypeSafe config library that makes it nice and easy to work with `application.conf` data
- ScalaTest for unit tests
- Mockito for mocking dependencies on socket related tests

### Dead letter queue
I decided to take a very basic and simple approach for the dead letter queue - it's simply a mutable `Queue` data structure.
Even though it is significantly slower than Vector (which is on the other hand the best choice for concurrent code) it was important for me to keep the FIFO functionality to avoid situations such as sending unfollow message before follow.

In production, I of course wouldn't have a dead letter queue in the application itself as it's not durable and hence defeats the purpose of storing the erroneous messages should the application fail. A great choice for that would be e.g. AWS SQS queue.
It's durable and can be monitored, there could be alerts set up that notify the team when there is an unusually high amount of dead letter queue entries - this could signify application is misbehaving.

I would store a reason for why each of the messages is being in dead letter queue (I already took the first step which is to model message related error case classes, see `EventError`). 

I would eventually replay the messages the are not delivered because the user was offline although I think that messages which are not delivered due to that reason don't really belong in a dead letter queue and definitely shouldn't be replayed blindly until success. Perhaps they should be stored in another queue which is pulled from on user log in (or in other words: on addition to the client pool)

I would purge the incorrect format messages, but not right away though. After some time when I know the average numbers of incorrect format messages (e.g. per day) I would set the alert on it. A large peak in those could indicate unhealthy behaviour from some clients which should be investigated. 

### Testing
Test the code with `sbt test`

Even though adding tests for part 1 was not in scope I decided to add a little bit for the new code I introduced. I like to have any converters/exporters thoroughly tested (especially if they are a brand new addition to the codebase). It's an area that can cause a lot of problems should edge cases scenarios not be properly covered. 
Speaking of this, you will notice one ignored test - as I didn't want to expand the time budgeted for the task any further I decided to leave as is. Please see `Improvements and notes` for explanation.

### Improvements and notes
Things I would not ship to prod as is:
- `MessageConverter.createEventInner` - this one would require more work and a proper parser + error handler. As mentioned above, I added a test case that I marked as ignored - sending a non-digit String would result in an Exception

Things I would definitely push to improve:
- definitely reduce (ideally: remove) the usage of mutable data structures. It would be ideal to use immutable structures.
- get rid of vars, which ties up with the first point too around mutability
- add better error handling and remove or significantly reduce the amount of `Unit`s returned.
- add logging instead of printing
- perhaps use a third party tool for dependency injection as the self type technique can create quite an obfuscated code very quick 
- use a third party library to wrap up IO code and enhance error handling, especially in for comprehensions (e.g. Cats)

### Difficulty
The task was a lot of fun and I feel like I learnt a little bit too as I haven't worked very close and/or tested socket communications before.

______________________


**Please [read the instructions first](INSTRUCTIONS.md).**

## Running the server

### go
```
cd go
go run main.go
```

### java
```
cd java
gradle run
```

### js
```
cd js
node index.js
```

### python
Please use a modern version of python 3, managed via a tool like pyenv.
```
cd python
python main.py
```

### ruby
Please use a modern version of cruby, managed via a tool like rvm.
```
cd ruby
ruby main.rb
```

### scala
```
cd scala
sbt run
```

## About the tester

The tester makes socket connections to the server:

- The event source connects on port 9090 and will start sending events as soon as the connection is accepted. 
- The user clients can connect on port 9099,
and communicate with server following events specification and rules outlined in challenge instructions. 

## Running the tester

From the project root, run:

`tester/run100k.sh`
