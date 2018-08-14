#!/usr/bin/env bash

# performs 'git branch -d {branch}' for all branches except current. If
# the branch isn't fully merged, the delete will fail with an error. So,
# this only deletes fully merged branches.
for b in $(git branch | awk 'NF==1{print}'); do
    git branch -d $b
done
git branch
