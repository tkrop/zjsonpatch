# Java JSON Patch library

Java JSON Patch library to create and apply JSON patches according to [RFC 6902](http://tools.ietf.org/html/rfc6902), based on Jackson 2.x JSON nodes.

A JSON Patch is defined by a JSON array describing the changes to a JSON document. It can be used to avoid sending a whole document when only a part has changed. It can be used to reduce the network bandwidth requirements, if the patch is smaller than the original resource. In addition, it may be used to concurrently apply none interfering patches on resources.

A JSON Patch is supposed to be combined with the HTTP PATCH method as defined in [RFC 5789 HTTP PATCH](http://tools.ietf.org/html/rfc5789). It will do the partial updates for HTTP APIs in a standard  way.

## Installation

The Java JSON Patch library is available on the Maven Cental repository. Add following to `<dependencies/>` section of your pom.xml:

```xml
<groupId>com.zalando.jsonpatch</groupId>
<artifactId>json-patch</artifactId>
<version>0.1.0</version>
```

**Note:** compatible with **Java 6, 7, and 8**

## Usage

### Patch Creation

Patches are created by the following method, that compute the patch from a given JSON source node and JSON target node:

```java
JsonNode patch = JsonPatch.create(source, target)
```

The source and target nodes can be any kind of JSON nodes, i.e. objects, arrays, strings, booleans, numbers, and null values. If the resulting patch is applied to source, it will yield target.

The algorithm computing the JSON patch creates ADD, REMOVE, REPLACE, and MOVE operations, if the patch optimization is activated.

### Patch Application

Patches are applied by the following request, that applies the patch to a target node and returning it:

```java
JsonNode target = JsonPatch.apply(patch, target);
```

The target node can be any kind of JSON node, i.e. objects, arrays, strings, booleans, numbers, and null values, while the patch must be an array of object describing the patch operations. If the target node is needed after application, it must be copied up-front.


## Performance Considerations

This JSON patch library is based on the very good [zjsonpatch](https://github.com/flipkart-incubator/zjsonpatch). Compared to it, it contains a number of considerable performance improvements, as reducing the problem size for the longest common sequence (LCS) algorithm, which improves the speed for our use case by a factor of **4** for patch creation. This can be improved to an factor of **8** by using the simplified compare patch generator without patch optimization which works perfect for structural stable documents.

## Complexity

### Patch Creation
- Creating patches of JSON objects has complexity of `Ω(N+M)`, where `N` and `M` represents number of keys in the source and the target JSON object.
- Creating patches of JSON arrays has complexity of `O(la*lb)`, where `la` and `lb` represent length of the source and the target array. Since a longest common sequence (LCS) algorithm is used to find difference between two JSON arrays the complexity is of worst case quadratic order. In practical use cases the complexity is reduced by linearly reducing the problem size.
- Optimizing patches, i.e. compact add and remove into move operations has complexity of `Ω(D)/O(D*D)`, where D represents the number of patch operations obtained before compaction.

### Patch Application
Application of patches has complexity of O(D), where D represents number of patch operations.

## Code Coverage
| Class %      | Method %       | Lines %        | Branches %      | Instructions %    |
|--------------|----------------|----------------|-----------------|-------------------|
| 100% (18/18) | 98.0% (98/100) | 100% (489/489) | 99.0% (208/210) | 99.6% (2335/2345) |
