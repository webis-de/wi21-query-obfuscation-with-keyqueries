# Efficient Query Obfuscation with Keyqueries

This repository contains data and source code for reproducing results of the paper (currently under review): Efficient Query Obfuscation with Keyqueries.

# Usage

Clone this repository:

```
git clone https://github.com/webis-de/wi21-query-obfuscation-with-keyqueries.git
cd wi21-query-obfuscation-with-keyqueries
```

Ensure, the project builds on your machine (uses [anserini](https://github.com/castorini/anserini) as dependency):

```
./compile-anserini.sh
```

Create the Anserini indices:
```
./crypsor-indexing/index_everything.sh
```

Run experiments with:

```
./arampatzis-hbc.sh
./run-all-cw12.sh
./run-all-cw09.sh
```

# Data for the User Study:

The data for the user study is located at [src/main/resources/crypsor-query-user-study/](src/main/resources/crypsor-query-user-study/).

# Plots and Tables in the Paper:

After running the experiments with the scripts above, you can obtain the plots and Tables in the paper via jupyter notebooks located in [src/main/jupyter/](src/main/jupyter/).

