# inmemory

Questions/Assumption:
The queue is a job queue or message queue (publisher-subscriber model like Kafka)? For example, when a message is added as an input and say we have 2 consumers. Do both consumers consume this and process this message ? Or does 1 of them processes it and other is remains idle? Assuming, it is publisher-subscriber.

Assuming out of delivery is ok

Assuming we start delivering to consumer only from point of subscription, no delivery of older messages

Assuming element is removed from queue iff is either processed or moved to DLQ. This maybe different from what example 1 & 2 talks about however this is inline with 1 point in this section.

What happens if the prereq consumer (i.e. A & B) do not have the filter expression which matches the expression of the dependent C.More precisely, the message is not processed by B due to filterining criteria. Assuming we still want to deliver as long as predecessor do not give errors.

Few improvments todo due to lack of time:

Retry mechanism is hardcoded to 3, can be improved and extracted as a user RetryPolicy 

Separate threadpool for sending the message to a single consumers. Need to take care of dependency

Optimizing/Generalizing the reaper logic

Avoiding circular dependency as that can casue code to go in recursive loop
