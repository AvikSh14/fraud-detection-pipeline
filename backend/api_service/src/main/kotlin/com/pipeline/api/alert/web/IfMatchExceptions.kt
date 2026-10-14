package com.pipeline.api.alert.web

// HTTP-level errors, so they live in the web layer; the domain never sees request headers

class MissingIfMatchException :
    RuntimeException("This request needs an If-Match header with the alert's ETag")

class InvalidIfMatchException(val headerValue: String) :
    RuntimeException("If-Match must be a strong ETag such as \"3\", but was $headerValue")
