#!/bin/bash
set -e

CURRENT_TAG="ver-1.21.5"
RELEASE_NOTES="RELEASE.md"

# Branch name
CURRENT_BRANCH=$(git rev-parse --abbrev-ref HEAD)
echo "✨Current branch: $CURRENT_BRANCH"

# Release notes header
echo "" >> $RELEASE_NOTES

# Commits log
{
  echo "### 📜 Commits:"
  echo ""
  echo "$COMMIT_LOG"
  echo ""
  echo "### 🔒 Checksums"
} >> $RELEASE_NOTES

# Get checksums
file="./pufferfish-server/build/libs/pufferfish-paperclip-1.21.5-R0.1-SNAPSHOT-mojmap.jar"
if [ -f $file ]; then
  SHA256=$(sha256sum $file | awk '{ print $1 }')
  SHA512=$(sha512sum $file | awk '{ print $1 }')
  FILENAME=$(basename $file)

  {
    echo "|           | $FILENAME |"
    echo "| --------- | --------- |"
    echo "| SHA256    | $SHA256   |"
    echo "| SHA512    | $SHA512   |"
  } >> $RELEASE_NOTES

  echo "🔒Checksums calculated:"
  echo "   SHA256: $SHA256"
  echo "   SHA512: $SHA512"
else
  echo "⚠️No artifacts found." >> $RELEASE_NOTES
fi

# Delete current release tag
if git show-ref --tags $CURRENT_TAG --quiet; then
  {
    gh release delete $CURRENT_TAG --cleanup-tag -y -R "${GITHUB_REPO}"
  }
fi
echo "🚀Ready for release"
