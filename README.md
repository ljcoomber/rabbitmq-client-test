PoC project to exercise some reliability features of RabbitMQ and client libraries.

Notes:

- a local instance of RabbitMQ must be running for all tests.
- relies on a snapshot build of  amqp-client

Whilst it is possible to automate some of the tests, they mostly involve doing atypical things (such as executing
arbitrary Erlang code in the broker) so have largely been left manual.

# Tests

## Automated Tests

See [the test folder](src/test/scala).

Whilst it is possible to automate some of the tests, they mostly involve doing atypical things (such as executing arbitrary Erlang code in the broker) so have largely been left manual.

## Re-connection Tests

1. Run TestPublisher
2. Every second, you should see a message on stdout indicating a message has been successfully published
3. Look at the connections in the [RabbitMQ Management Console](http://localhost:15672/#/connections)
4. Select the connection and click 'Force Close' at the bottom of the page
5. In the console, you should see errors due to the connection closing, and then re-establishment of the connection and  channel, and successful message publishing continuing

# TODO

- [X] Basic publisher
- [ ] Basic consumer
- [ ] Re-connection
- [X] Transactions
  - [X] Success
  - [X] Failure
- [ ] Publisher confirms
- [ ] Queue size
- [ ] Connection / channel status
- [ ] Multiple channels

To help with testing, see http://rabbitmq.1065348.n5.nabble.com/Testing-lightweight-publisher-confirms-td834.html 
