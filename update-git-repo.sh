#!/bin/bash

echo "========================================"
echo "Updating SQLServerScripts Git Repository"
echo "========================================"
echo

# Change to the script's directory (in case it's run from elsewhere)
cd "$(dirname "$0")"

echo "Current directory: $(pwd)"
echo

# Check if this is a git repository
if ! git status &>/dev/null; then
    echo "ERROR: This directory is not a Git repository!"
    echo "Please ensure you're in the correct directory."
    read -p "Press any key to exit..."
    exit 1
fi

echo "Step 1: Fetching latest changes from remote..."
if ! git fetch --all; then
    echo "ERROR: Failed to fetch from remote!"
    read -p "Press any key to exit..."
    exit 1
fi

echo
echo "Step 2: Checking current branch..."
current_branch=$(git branch --show-current)
echo "Current branch: $current_branch"

echo
echo "Step 3: Checking for uncommitted changes..."
if ! git diff-index --quiet HEAD --; then
    echo "WARNING: You have uncommitted changes!"
    echo
    git status --short
    echo
    read -p "Do you want to commit these changes? (y/n): " commit_changes
    if [[ "$commit_changes" =~ ^[Yy]$ ]]; then
        read -p "Enter commit message: " commit_message
        git add -A
        if ! git commit -m "$commit_message"; then
            echo "ERROR: Commit failed!"
            read -p "Press any key to exit..."
            exit 1
        fi
    else
        echo "Proceeding without committing changes..."
    fi
fi

echo
echo "Step 4: Pulling latest changes..."
if ! git pull origin "$current_branch"; then
    echo "ERROR: Pull failed! You may have merge conflicts."
    echo "Please resolve conflicts manually."
    read -p "Press any key to exit..."
    exit 1
fi

echo
echo "Step 5: Pushing any local commits..."
if ! git push origin "$current_branch"; then
    echo "WARNING: Push failed! You may need to pull first or resolve conflicts."
    read -p "Press any key to exit..."
    exit 1
fi

echo
echo "========================================"
echo "Repository update completed successfully!"
echo "========================================"
echo
echo "Current status:"
git status --short --branch
echo
read -p "Press any key to exit..."
