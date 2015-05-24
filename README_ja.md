Job Copy Builder plugin
=======================

ジョブをコピーするビルド手順を追加するJenkinsプラグイン

これはなに？
------------

Job Copy Builder は、「ジョブをコピーする」ビルド手順を追加する [Jenkins](http://jenkins-ci.org/) プラグインです: 

* 既存のジョブから新しいジョブを作成します。
	* ビルド手順として設定できるので、複数のビルド手順を追加することで1度のビルドで複数のジョブをコピーできます。
* 以下のパラメータを設定します:
	* コピー元のジョブ
		* 変数を使用できます
	* コピーして作成するジョブ
		* 変数を使用できます
	* 上書きする
		* コピー先のジョブが既に存在する場合に、ジョブを上書きするかどうかを指定します。
* ジョブをコピーするときに追加で行う処理を指定できます。
	* ジョブを有効にする: コピー元のジョブが無効になっている場合に、コピー先のジョブを有効に設定します。
	* ジョブを無効にする: コピー元のジョブが有効になっている場合に、コピー先のジョブを無効に設定します。
	* 文字列を置き換える: ジョブの設定に含まれる文字列を置換します。
		* 置換元、置換先の文字列には変数を使用できます。
* 追加で行う処理は[Jenkinsの拡張ポイント機能] (https://wiki.jenkins-ci.org/display/JENKINS/Extension+points) を使用して新しいものを追加することができます。

制限事項
--------

* 「ジョブをコピーする」ビルド手順を設定したジョブはマスターノードで実行する必要があります。

このプラグインの動作原理
------------------------

このプラグインは以下のように動作します:

1. コピー元のジョブの設定XML (config.xml) を読み込む。
2. 追加の処理を設定XMLの文字列に適用する。
3. 変換後のXML文字列から新しいジョブを作る。

拡張ポイント
------------

新しい追加の処理を作る場合は、`JobcopyOperation` 抽象クラスを拡張し、以下のメソッドをオーバーライドします:

```java
public abstract String JobcopyOperation::perform(String xmlString, String encoding, EnvVars env, PrintStream logger);
```

もしくは、`AbstractXmlJobcopyOperation` 抽象クラスを使って以下のメソッドをオーバーライドすれば、XML Documentオブジェクトを使った処理をすることができます:

```java
public abstract String AbstractXmlJobcopyOperation::perform(Document doc, EnvVars env, PrintStream logger);
```

TODO
----

* 正規表現版の文字列の置き換えを作る

