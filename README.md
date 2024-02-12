# regurgitator-extensions-swagger

regurgitator is a lightweight, modular, extendable java framework that you configure to 'regurgitate' canned or clever responses to incoming requests; useful for quickly mocking or prototyping services without writing any code. simply configure, deploy and run.

start your reading here: [regurgitator-all](https://talmeym.github.io/regurgitator-all#regurgitator)

[``apidocs``](https://regurgitator.emarte.uk/apidocs/regurgitator-extensions-swagger/0.1.3/)

## regurgitator and swagger

generate regurgitator configuration from open api / swagger files using regurgitator-extensions-swagger

```xml
<dependency>
    <groupId>uk.emarte.regurgitator</groupId>
    <artifactId>regurgitator-extensions-swagger</artifactId>
    <version>0.1.2</version>
</dependency>
```

functionality includes:
- request routing via top level decision step
- generation of example request and response files
- extraction of path variable parameters using extract processor

*currently only json configuration and output files supported*

*more functionality to come - under construction*

**go from a single open api file to a working mock returning responses for every method/path combination (without writing any code)**

**extend what it generates to add simple or complex logic to your mock (without writing any code)**

```java uk.emarte.regurgitator.extensions.swagger.ConfigurationGenerator swaggerfile.[json|yaml] outputDirectory```
