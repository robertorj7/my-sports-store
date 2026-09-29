# my-sports-app-web

Frontend Angular 22 da My Sports Store.

## Rodando

1. Suba a API (`my-sports-store-api`) em `http://localhost:8080`.
2. `npm install`
3. `npm start` e acesse `http://localhost:4200`.

As chamadas para `/api` são redirecionadas para a API pelo `proxy.conf.json`, então não é preciso configurar CORS em desenvolvimento.

O usuário admin é criado pelo DataSeeder a partir das variáveis `ADMIN_EMAIL` e `ADMIN_PASSWORD` da API.

## Telas

- `/login` e `/register` — autenticação (JWT salvo no `localStorage`)
- `/products` — catálogo com filtro por categoria e busca
- `/cart` — carrinho e finalização de compra
- `/orders` — pedidos do usuário

Todas as rotas exceto login/cadastro exigem autenticação (`authGuard`). O `authInterceptor` adiciona o header `Authorization: Bearer <token>` e faz logout em caso de 401.

## Testes

`npm test`
