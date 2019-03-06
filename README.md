# resolve-parent-version-maven-plugin

This plugin updates a pom file with the resolved parent version. That is, it ensures that for a pom
that has a parent pom reference, that when it gets installed/deployed, any property placeholders in the parent
pom version field will be resolved.

See this blog post for more details: https://blog.kiwitype.com/2017/maven-versioning-in-a-continuous-delivery-pipeline/

e.g.
Say we have the following pom:

```xml
<project>
  <parent>
    <groupId>com.mycompany.app</groupId>
    <artifactId>my-app</artifactId>
    <version>1.${some.property}</version>
  </parent>
 
  <artifactId>my-module</artifactId>
 
  ...
 
</project>
```
 
 If a build runs where some.property=999, then the installed/deployed pom will be:
 
```xml
<project>
  <parent>
    <groupId>com.mycompany.app</groupId>
    <artifactId>my-app</artifactId>
    <version>1.999</version>
  </parent>
 
  <artifactId>my-module</artifactId>
 
  ...
 
</project>
```
## Usage

```xml
 <plugin>
  <groupId>com.hbakkum.maven.plugins</groupId>
  <artifactId>resolve-parent-version-maven-plugin</artifactId>
  <version>0.8</version>
  <executions>
    <execution>
      <goals>
        <goal>resolve-parent-version</goal>
      </goals>
    </execution>
  </executions>
</plugin>
```
