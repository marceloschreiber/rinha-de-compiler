# Rinha in Clojure

Interpretador para a [rinha de compiler](https://github.com/aripiprazole/rinha-de-compiler). É o primeiro interpretador que faço, então só fiz da maneira mais direta possível.

Fiz quase 0 testes mas implementei tail call.

## Rodando

Criar um arquivo em `"/var/rinha/source.rinha.json"` e executar:

```shell
clj -M -m core
```

## Docker

Criar a imagem:

```shell
docker build . -t marcelo-clojure-rinha
```

rodar passando o arquivo como um volume:

```shell
docker run -v ./resources/fib.rinha.json:/var/rinha/source.rinha.json marcelo-clojure-rinha
```
