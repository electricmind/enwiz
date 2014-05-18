English Wizard
==================


This very tool is just a toy, that can  work on the top of a serious dataset about millions of English trigrams, that comes from a range of texts. It provides two opportunities: a bit of enjoy and a bit of better sense of how English really runs.

First one is a **mnemonic generator**, theoretically one can use it to memorize a long sequence of numbers.  Let's assume you need to memorize a Pi. Look at the lengths of the words: *"And(3) I (1) have (4) a (1) child (5) condemned (9) to (2) always (6) think (5)."*, that gives 3.14159265. Practically, just because English is not my native, it works for me in opposite direction, that's enjoyable.

One can write a bunch of amusing things: *"They were both gone into some kind that says that this time when they were gone from them both with guns they find this town ."*, what a lurid story of two awful fellows in four-letter words!

For the lack of an interest to numbers, one can focus on language itself with a sentence generator. The tool suggests more plausible word with respect to a couple of previous. English writing becomes a lot of easy for one, who chooses suitable words one by one. Of course it would be a complete synthetic nonsense, but .... you will be surprised that only a little participation of humanity is really necessary to put a sense into them.

As for me, it helps to estimate possible context of different words or possible language constructs, but to be honest I used a little more sophisticated tool.

Two words about grammar correctness. For each three word that database uses it's guaranteed that they comes from a real text, correct one. For each four-five word that is quite possible to find a text that contains them. More than five .... yeah, it's debatable.


Sample of vocabulary
---------------------
Just to illustrate the claim about women romance, I show a statistic of two source of data: Wikipedia articles related to the word "Forensic" and a bunch of fiction book:

Forensic set :

| length  | unigram | bigram | trigram  | average frequency of trigramm |
| --------| ------ | ------- | -------- | ----------------------------- |
|  242M   | 101954 | 1080966 | 2.405365 | 1.89                          |

Fiction set :

|  length | unigram | bigram | trigram  | average frequency of trigramm |
| --------| ------ | ------- | -------- | ----------------------------- |
|  87M    | 85044  | 1343235 | 3799617  | 2.37                          |

Considering the fact, that size of any corpora is limited  , less intellectual source provides a sample with higher average frequency of trigramm having less or the same vocabulary size. Average frequency of trigramm is a crucial parameter that reflects a quality of model. Text generated on the base of the model with large average frequency of trigramm better come up to a human expectations. So, difference between 1.89 and 2.37 is the difference between two mnemonics of Pi : *"But I have a great diversity of people using the Latin American president .* and *And I have a child condemned to always think .*
 
So, women romance is going to be an incredible source of data for natural language processing tools :).

<!--
|         | unigram | bigram | trigram  | frequency |
| --------| ------ | ------- | -------- | --------- |
| xaa-xae | 33000  |  199000 | 329000   | 1.37      |
| xaf-xaj | 45461  |  324267 | 579255   | 1.482     |
| xak-xao | 54941  |  434926 | 817300   | 1.521     |
| xau-xay | 63841  |  541313 | 1051161  | 1.56      |
| xay-xbc | 70783  |  631158 | 1259202  | 1.646     |
| xbd-xbh | 78613  |  738428 | 1515427  | 1.69      |
| xbi-xbm | 85038  |  825300 | 1728236  | 1.721     |
| xbn-xbr | 85038  |  825300 | 1728236  | 1.76      |
| xbs-xbw | 88164  |  873820 | 1854607  |  1.79     |
| xbx-xcb | 92218  |  929691 | 1993352  | 1.808     |
| xcc-xcg | 97179  | 1003502 | 2193309  | 1.864     | 
| xch-xci | 101954 | 1080966 | 2.405365 | 1.89      |


-->

<!--
The Wikipedia is an atypical case though: for fiction books a vocabulary grown shows different shapes:

|         | unigram | bigram | trigram  | frequency |
| --------| ------ | ------- | -------- | --------- |
|  xaa    | 25228  | 226915  |  485915  | 1.664     |
|  xab    | 33411  | 381919  |  905278  | 1.862     |
|  xac    | 41397  | 516805  | 1282582  | 1.967     |
|  xad    | 51640  | 667635  | 1692993  | 2         |
|  xae    | 58162  | 782039  | 2032108  | 2.053     |
|  xaf    | 64081  | 895827  | 2381007  | 2.111     |
|  xag    | 69331  | 1004366 | 2721126  | 2.157     |
|  xah    | 74191  | 1110674 | 3057782  | 2.221     |
|  xai    | 78598  | 1205567 | 3356044  | 2.280     |
|  xaj    | 80744  / 1260286 / 3538706  / 2.309     / 
|  xaj    | 82847 / 1295670 / 3645601 / 2.320     /
/  xak    | 85044 | 1343235 | 3799617 |"2.3741653435069905) |
|         |        |         |          |           |

(Each individaul sample was about 8M length, 87M in common, frequency is for average frequency of trigram).
-->


A few outcomes of the mnemonic generator
-------------------------------------------

 - Pi = 3.14159265 :	*And I have a child condemned to always think .*, *Now I have a rectangle of public space .*
 - Pi =3.141592653589	 : "But I have a great diversity of people using the Latin American president .*
 -	e = 2.71828 : *He thought a headache of migraine.*
 -	e = 2.71828182845 : *To achieve a majority of building a database is designed with space .*
 -	just numbers = 242424242424242424242424 : 	*He felt as well as some of them in that it made me feel as much as they do that to make it easy .*
 - just numbers = 1234567654321 :	*I do not know about thirty seconds before their eyes met in a ..*
 - just numbers = 444444444444444444444444 : *They were both gone into some kind that says that this time when they were gone from them both with guns they find this town .*


Usage
-------------
What you need is a mongodb and a text corpora. Large text corpora contains a lot of words and so forth, that 
is pretty wonderful to threaten your classmates with a real power of your laptop, but, the problem is, that:

 - more important is an amount of sentences that involves each of the words

 - interactively, you barely would possible to operate with a choice even one of one hundred words.

So, of course I would suggest to use a large vocabulary from a range of different texts, but just for your expertise, more interesting point is a large text corpora that contains relatively small vocabulary. Something like women romance is quite suitable for that, try to feed the tool with one ... hundred of them.

Having a shelves of texts, mongodb, running program and browser opened, enter the texts into the upload form one by one. It's quickly cramming :).

Build & Run
-------------------
This script rely on MongoDB. Create one and olace two configures
enwizdb.cfg and enwizauth.cfg into your home directory:

 - enwizdb.cfg contains varaibles dbname, port, user, password and host to connect
   database;

 - enwizauth.cfg contains variables username and password to grant access to load 
   text sample into database.

Then run following script: 

```sh
$ cd EnWiz
$ ./sbt
> container:start
> browse
```

Environment should contain variables JELASTIC_USERNAME and JELASTIC_PWD to deploy (with command deploy) 
script on jelastica cloud:

```
export JELASTIC_PWD="12345678"
export JELASTIC_USERNAME="user@example.org"
sbt package-war
sbt deploy
```

If `browse` doesn't launch your browser, manually open [http://localhost:8080/](http://localhost:8080/) in your browser.


History
---------
Version 0.1.0, 20140518, Initial release

