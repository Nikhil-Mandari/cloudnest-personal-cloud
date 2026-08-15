#!/usr/bin/env bash
# CloudNest — Azure VM provisioning (run on your local machine with az CLI logged in)
# Usage: bash deploy/azure/provision-vm.sh [resource-group] [vm-name] [location] [size]
# Example: bash deploy/azure/provision-vm.sh cloudnest-rg cloudnest-vm centralindia Standard_B2ms
#
# Safe: only creates Azure resources. Opens ONLY 80/443/22. Never exposes 3306/8080-8087/8761/8888/9000/9001.
set -euo pipefail

RG="${1:-cloudnest-rg}"
VM="${2:-cloudnest-vm}"
LOC="${3:-centralindia}"
SIZE="${4:-Standard_B2ms}"
ADMIN="${5:-azureuser}"

command -v az >/dev/null || { echo "ERROR: az CLI not found. Install: https://learn.microsoft.com/cli/azure/install-azure-cli"; exit 1; }

echo "==> Checking Azure login..."
az account show >/dev/null 2>&1 || { echo "ERROR: not logged in. Run: az login"; exit 1; }

echo "==> Creating resource group $RG ($LOC)..."
az group create --name "$RG" --location "$LOC" --output none

echo "==> Creating VM $VM ($SIZE, Ubuntu LTS, SSH auth)..."
az vm create \
  --resource-group "$RG" \
  --name "$VM" \
  --image Ubuntu2204 \
  --size "$SIZE" \
  --admin-username "$ADMIN" \
  --generate-ssh-keys \
  --nsg-rule SSH \
  --output none

echo "==> Opening only 80 and 443 (SSH 22 already open by --nsg-rule SSH)..."
az vm open-port --resource-group "$RG" --name "$VM" --port 80 --output none
az vm open-port --resource-group "$RG" --name "$VM" --port 443 --output none

PUBLIC_IP=$(az vm show --resource-group "$RG" --name "$VM" --show-details --query publicIps -o tsv)
echo
echo "==> VM ready."
echo "   Resource group : $RG"
echo "   VM             : $VM"
echo "   Public IP      : $PUBLIC_IP"
echo "   SSH            : ssh $ADMIN@$PUBLIC_IP"

# Copy the setup script to the VM so it is available before the repo clone.
echo "==> Copying setup-vm.sh to the VM..."
scp -o StrictHostKeyChecking=accept-new deploy/azure/setup-vm.sh "$ADMIN@$PUBLIC_IP:setup-vm.sh"

echo
echo "==> Next steps:"
echo "   1) Point your staging DNS name at $PUBLIC_IP"
echo "   2) ssh $ADMIN@$PUBLIC_IP"
echo "   3) bash setup-vm.sh <staging-domain>   (run on the VM; it clones main, scaffolds .env, deploys)"
