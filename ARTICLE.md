# Compiler vos applications Java en exécutables natifs

---
- quelles sont les limitations ?
- comment ca marche ?
- comment adapter votre projet

[code source example](https://github.com/eflorent-ineat/green-it/)

----

Avoir des applications très performantes est un moyen de faire de l'informatique durable. 
Depuis quelques temps je m'intérresse à la performance des applications et, comme développeurs Java,
je me suis naturellement tourné vers GraalVM pour améliorer la performance de mes microservices.

GraalVM est un de mes outils d'informaticien eco-responsable. Voici son histoire:

- 14 Janvier 2020, GraalVM `19.3.1` 1er version LTS, ajoute le support de Java 11,
- 17 avril 2018, GraalVM `19.0`, 1ere version stable,
- 31 Juillet 2012, Oracle à propos de ses recherche sur Truffle,

car en effet GraalVM utilise Truffle, un projet sans équivalent.
Truffle est un framework d'interpretation mutli-languages.
C'est un environnement d'execution auto-optimisé,
Plus de 40 ingénieurs Oracle on travaillé pendant 3 ans sur le projet GraalVM un compilateur et Truffle outils AST
(Abstract Syntax Tree) afin de produire une version stable.

Je vous propose de partager mes recettes de compilation avec cette dernière version et de profiter de la révolution native:

- des déploiements trop pratiques: un simple exécutable, de seulement quelques mega, 
n'a pas besoin de JVM ou de dépendances, il va être complètement indépendant (standalone EXE), 
comme en GoLang.
- une empreinte mémoire  5 fois plus faible.
- [des temps de démarage 50 fois plus rapides](https://www.graalvm.org/docs/why-graal/#for-microservices-frameworks).
- la possibilité d'intégrer d'autres langages dans l'exécutable (Node, Ruby, R, Python, C++)



## Quelles sont les limitations

Par design, n'importe quelle application peut être compilée mais il y des différences , 
en particulier l'introspection n'est pas possible. Il y a au départ une analyse statique du code. 
Toutefois il est possible de donner un indice au compilateur: la liste des classes supplémentaires à charger,
et un outil pour lister les classes qui ne peuvent pas être détéctée par analyse statique du code mais par analyse dynamique.

 Personnellement je n'ai trouvé cette limitation très difficile. Par exemple en utilisant ORMLite, 
 toutes les classes DAO doivent être renseignées ce n'est pas trop gênant une fois que l'on a compris.
 Jetty marche tous seul par exemple. Sur un projet Java 8 j'ai pu avoir a notifier pour 
 `org.joda.time.DateTime` ou `org.h2.engine.Engine` car ils utilisent l'introspection. 
 [Voici un exemple](https://gist.github.com/eflorent-ineat/eec780e5ecb53a39c0c2f681671f31ce) de configuration du compilateur AOT GraalVM issue d'un projet réel.

Les executables produits par GraalVM ciblent un plateforme  parmis:
- amd64 linux
- amd64 windows
- amd64 darwin

Donc les applications Java compilées ne sont pas portables mais peuvent être compilées pour les cibles courantes.

Dans cet exemple je ne construit que des executables amd64 Linux bien que la meme application puisse etre compilée 
pour MacOS (darwin) et Windows sur Azure DevOps par exemple.

Je vous propose quelques explications et les outils pour:
  
 - construire localement des exécutables via docker pour une faible empreinte
 - construire un environnement d'intégration continue pour déployer des  exectables natifs à partir de code Java 11 (et en dessous)

## Comment ça marche
 
GraalVM propose au développeurs Java deux outils essentiels, `native-image` et `native-image-agent` 
Il y a aussi un bien pratique `native-image-maven-plugin`.
   
 - `native-image` , va parcourir *statiquement* votre code, celui des dépendances, et
 du JDK et va passer l'ensemble du code 
 au [compilateur GraalVM](https://www.graalvm.org/docs/reference-manual/native-image/#graalvm-native-image).
    
 L'instanciation statique et la compilation par avance (Ahead Of Time ou AOT) va avoir 
 [des limittes](https://www.graalvm.org/docs/reference-manual/native-image/#tracing-agent) mais on peut les dépasser: 
 - l'utilitaire `native-image-agent`  ou [Tracing Agent](https://www.graalvm.org/docs/reference-manual/native-image/#tracing-agent)
  permet à partir d'un jar intermédiaire, de compléter les classes et les ressources appelées 
  par *instanciation dynamique*, c'est à dire par l'application en marche.
   Par exemple vous pourrez dérouler une suite de tests avec le tracing agent pour repérer les
   classes appelées par introspection.
- Enfin,  `native-image-maven-plugin` recherche automatiquement un fichier  
 `src/main/resources/META-INF/native-image/${groupId}/${artifactId}/native-image.properties`
 et utilise son contenu, vous trouverez un exemple contenant les options essentielles  dans
 le répertoire git lié à cet article.
 
# comment adapter votre projet étape par étape

- ajouter une dépendance Maven facultative, utile pour `native-image-agent`:

```
<dependencies>
    <dependency>
        <groupId>com.oracle.substratevm</groupId>
        <artifactId>svm</artifactId>
        <version>${graal.version}</version>
        <scope>provided</scope>
    </dependency>
</dependencies>
```

- ajouter le `native-image-maven-plugin`

```
<plugin>
    <groupId>com.oracle.substratevm</groupId>
    <artifactId>native-image-maven-plugin</artifactId>
    <version>${graal.version}</version>
    <executions>
        <execution>
            <goals>
                <goal>native-image</goal>
            </goals>
            <phase>package</phase>
        </execution>
    </executions>
</plugin>
```
- ajouter une recette de construction `Dockerfile.graalvm`, telle que dans le répertoire Git de ce projet

- Spécifier la classe main dans votre `pom.xml`, façon Maven:
```
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-jar-plugin</artifactId>
    <configuration>
        <archive>
            <manifest>
                <addClasspath>true</addClasspath>
                <mainClass>groupId.artifactId.VotreMain</mainClass>
            </manifest>
        </archive>
    </configuration>
</plugin>
```

- ajouter un répertoire: `src/main/resources/META-INF/native-image/$groupId/$artifactId/` et copier 
les fichier exemples fournis. Sans erreur, l'application générée s'appellera "app" si le fichier est 
pris en compte, sinon "package.main" si le fichier n'est pas trouvé au bon endroit.

- Ajouter comme dans le projet lié a ce git un `Makefile`, `Dockerfile` et `Dockerfile.graalvm` 
et un fichier `docker-compose.build.yml`


- vous pouvez maintenant préparer votre container de build:

```
make prepare
make clean native
```

Et voilà votre exécutable se trouve dans `target/app` si vous avez cloné [le projet exemple](https://github.com/eflorent-ineat/green-it/).

La principale difficulté que vous rencontrerez sera avec les introspections, mais là encore pas de problème!
Munissez vous de votre classpath complet (votre IDE l'affiche à la compilation) et adapter
le classpath a la ligne ou il y a `-cp ...`, exécuter ensuite `make configure-native`. Tandis que votre 
application s'exécute (vous pouvez lui faire réaliser quelques action), `native-agent` va écrire pour vous les 
introspections. exécuter les tests unitaires ici.


Vous rencontrerez surement quelques questions, mais le support sur le web est excellent, vous trouverez 
surement la bonne réponse, par exemple sous Mac vous pouvez être amené a augmenter la mémoire allouée a Docker 
pour l'étape de compilation développeur (rappelez vous l'empreinte mémoire de votre application va être divisée par 5)

Voilà pour finir je vous propose une intégration dans une pipeline Drone CI dans le fichier `drone.yaml` en quelques étapes:

- la première étape vous dote d'une image Docker avec GraalVM et Maven

```
- name: prepare
    image: docker:latest
    privileged: true
    volumes:
        - name: dockersock
        path: /var/run/docker.sock
    commands:
        - docker build -t graalvm:snapshot -f Dockerfile.graalvm .

```

- la deuxième étape appelle la compilation, simplement:

````
- name: build
    image: graalvm:snapshot
    commands:
        - mvn package
`````

Vous pouvez au besoin construire un container de la sorte avec l'appel de `make container` :

```
FROM scratch
COPY /data /data
COPY target/app /app
CMD ["/app"]
```

Sentez vous libre de cloner le projet disponible ici, de poser des questions ou de proposer des solutions complémentaires.


 
 


