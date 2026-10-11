# Ekayn API

API Spring Boot + PostgreSQL pour les comptes clients, les commandes et l’administration.

## Démarrer

```bash
docker compose up -d postgres mailpit
mvn spring-boot:run
```

Configuration utile : `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`, `JWT_SECRET`, `STRIPE_SECRET_KEY` et `STRIPE_WEBHOOK_SECRET`. Le secret JWT est obligatoire, aléatoire et doit faire au moins 32 caractères. Flyway applique les migrations PostgreSQL ; Hibernate est en mode `validate`.

En production, définir aussi `FRONTEND_URLS=https://ekayn.com,https://www.ekayn.com`, `COOKIE_SECURE=true`, `MAIL_HOST`, `MAIL_PORT` et `MAIL_FROM`. Le backend autorise cette liste en CORS avec credentials. En local, Mailpit est disponible sur http://localhost:8025.

## Routes principales

- `POST /api/auth/register`, `POST /api/auth/login`, `POST /api/auth/logout`
- `GET/PATCH/DELETE /api/me`, `GET/POST/PATCH/DELETE /api/me/addresses/{id}`
- `GET /api/orders`, `GET /api/orders/{id}`, `POST /api/checkout/session` (retourne uniquement `checkoutUrl` et accepte `Idempotency-Key`)
- `GET /api/admin/orders`, `PATCH /api/admin/orders/{id}/delivery`
- `POST /api/stripe/webhook`

Livraison France métropolitaine : `POST /api/checkout/quote` puis `POST /api/checkout/session` utilisent uniquement `shippingMethod: STANDARD_HOME_DELIVERY` et l’`addressId` du client. Les frais sont de 6,90 €, gratuits dès 69 € de sous-total ; devis et total sont calculés exclusivement par le serveur. La commande conserve `shippingAddressSnapshot`. La mise à jour admin `PATCH /api/admin/orders/{id}/delivery` accepte `deliveryStatus`, `carrier`, `trackingNumber`, `trackingUrl` et `shippedAt`. La notification d’expédition est envoyée une seule fois lors du passage à `EXPEDIEE`.

Les sessions utilisent un cookie HttpOnly `ekayn_session`, avec cookie CSRF associé. Les prix envoyés par le navigateur sont ignorés : le serveur recalcule le total depuis son catalogue. Une commande contient ses propres snapshots de lignes et d’adresse. `paymentStatus` est mis à jour uniquement par le webhook Stripe ; `deliveryStatus` est modifiable uniquement par un administrateur, sauf le passage initial à `CONFIRMED` effectué par le webhook après paiement. Les cartes ne sont jamais enregistrées.

Pour créer le premier administrateur, inscrire un compte puis modifier son rôle en base :

```sql
update users set role = 'ADMIN' where email = 'admin@ekayn.com';
```
