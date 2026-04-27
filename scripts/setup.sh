#!/bin/bash
# scripts/setup.sh - AeroStride Development Environment Setup

set -euo pipefail

echo "🚀 Setting up AeroStride development environment..."

# Check prerequisites
check_cmd() {
    if ! command -v "$1" &> /dev/null; then
        echo "❌ $1 is not installed. Please install it first."
        exit 1
    fi
}

check_cmd "java"
check_cmd "node"
check_cmd "npm"
check_cmd "docker"

# Setup Backend
echo "📦 Setting up Backend (BE)..."
cd BE
chmod +x gradlew
./gradlew build -x test
cd ..

# Setup Frontend
echo "📦 Setting up Frontend (FE)..."
cd FE
npm install
cd ..

# Copy example env files if not exist
if [ ! -f .env ]; then
    echo "📄 Creating .env from .env.example..."
    cp FE/.env.example .env || true
fi

echo "✅ Setup complete! You can now run the project using:"
echo "   docker compose -f docker/docker-compose.yml up -d"
