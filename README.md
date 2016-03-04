Maven Delimiter Crypto Plugin
==================================

The plugin *crypto-delimiter-maven-plugin* lets you encrypt and decrypt source text files within start and end delimiters.

Note: encrypted text is base64 encoded to allow ediing with text editor


Overview
--------------------


1) create a maven project with pom.xml indicating files to encrypt via filesets and cipherOptions

<project>
  <build>
   <defaultGoal>org.osa:crypto-delimiter-maven-plugin:crypto</defaultGoal>
    <plugins>
      <plugin>
        <groupId>org.osa</groupId>
        <artifactId>crypto-delimiter-maven-plugin</artifactId>
        <version>0.4-SNAPSHOT</version>
        <configuration>
          <fileSets>
            <fileSet>
              <directory>src/test/example</directory>
              <!--<outputDirectory>/</outputDirectory> -->
              <include>*.properties</include>
            </fileSet>
          </fileSets>
          <cipherOptions>
            <operationMode>encrypt</operationMode>
            <algorithm>AES</algorithm>
            <algorithmMode>ECB</algorithmMode>
            <algorithmPadding>PKCS5Padding</algorithmPadding>
            <secret>4IGGEPu/81QQxC62yOuFoQ==</secret>
            <startDelimiter>ENC(</startDelimiter>
            <endDelimiter>)</endDelimiter>
            <keepDelimiters>true</keepDelimiters>
          </cipherOptions>
        </configuration>
      </plugin>
    </plugins>
  </build>
</project>

see [wikipedia Base64 specification][site]
[site]https://en.wikipedia.org/wiki/Base64

see src/it/encrypt and src/it/decrypt for examples within source code

Development
-----------

* Build the plugin

    mvn clean install

  Make sure you got [Maven 3.2.1+][maven_download] or higher.

* Build the site (and the optional example report)

    mvn clean install integration-test site -Psite

    mvn site:deploy -Psite,dist-labs

* Release

    mvn release:prepare

    mvn release:perform

Make sure you got the changes etc for the site updated previous to the release.

[maven_download]: http://maven.apache.org
