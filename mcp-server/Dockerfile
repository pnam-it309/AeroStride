FROM node:18-alpine AS builder

WORKDIR /app

COPY package*.json ./
RUN npm install --ignore-scripts

COPY tsconfig.json ./
COPY src/ ./src/

RUN npm run build

FROM node:18-alpine AS runtime

WORKDIR /app

COPY package*.json ./
RUN npm install --only=production --ignore-scripts

COPY --from=builder /app/dist ./dist

EXPOSE 3000

USER node

CMD ["npm", "start"]
