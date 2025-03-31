# regurgitator-extensions-swagger

regurgitator is a lightweight, modular, extendable java framework that you configure to 'regurgitate' canned or clever responses to incoming requests; useful for quickly mocking or prototyping services without writing any code. simply configure, deploy and run.

start your reading here: [regurgitator-all](https://talmeym.github.io/regurgitator-all#regurgitator)

## regurgitator and swagger

generate regurgitator configuration from open api / swagger files

```xml
<dependency>
    <groupId>uk.emarte.regurgitator</groupId>
    <artifactId>regurgitator-extensions-swagger</artifactId>
    <version>0.1.5</version>
</dependency>
```

### functionality

- parsing of json and yaml open api / swagger files
- request routing handled via top level decision step
- generation of example request and response files (in json or xml, as specified in swagger)
- extraction of path and query parameters using extract processor
- json and xml configuration output now supported
- generation of corresponding postman request collection for testing

### benefits

**>> go from a single open api file to a working mock for every method/path combination ..**

**>> extend what is generated to add simple or complex logic to your mock ..**

**>> test every method/path combination with Postman ..**

**... all without writing any code**

### usage

```java uk.emarte.regurgitator.extensions.swagger.ConfigurationGenerator swaggerfile.[json|yaml] outputDirectory outputType [json|xml]```

NOTE: **regurgitator-extensions-swagger** works with **regurgitator-core** version **0.1.3+**

---

api docs: [``0.1.5``](https://regurgitator.emarte.uk/apidocs/regurgitator-extensions-swagger/0.1.5/){:target="_blank"}  [``0.1.4``](https://regurgitator.emarte.uk/apidocs/regurgitator-extensions-swagger/0.1.4/){:target="_blank"}  [``0.1.3``](https://regurgitator.emarte.uk/apidocs/regurgitator-extensions-swagger/0.1.3/){:target="_blank"}  [``0.1.2``](https://regurgitator.emarte.uk/apidocs/regurgitator-extensions-swagger/0.1.2/){:target="_blank"} 
