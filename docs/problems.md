# Problem types

Every error response is `application/problem+json` ([RFC 9457](https://www.rfc-editor.org/rfc/rfc9457)). The `type` field is one of the links below, and clients can rely on it. The `detail` text is for people and may change.

## not-found

`404`. No dispute or transaction with that id or reference.

## dispute-refused

`422`. The policy does not allow this dispute: the amount is more than the payment or zero, the currency differs, or the payment is older than 120 days. The model is not called.

## idempotency-key-reused

`422`. The `Idempotency-Key` was already used for a different request. Use a new key for a new request.

## illegal-transition

`409`. The dispute's status does not allow this step, for example resolving before evidence was submitted. The response for a dispute lists `allowedNext`.

## concurrent-update

`409`. Someone else changed the dispute after you read it. Read it again and retry.

## invalid-value

`400`. A value passed the shape checks but is still invalid, for example an amount with more than two decimals.

Framework errors, such as malformed JSON, a missing header or a failed validation, are also `400` problems, without a custom type.
