#!/bin/sh
git checkout enterprise-edition
git merge --log --no-edit master
git checkout local-test-basic-auth
git merge --log --no-edit master
git checkout local-test-basic-auth-ee
git merge --log --no-edit master
git checkout local-test-basic-auth-groups
git merge --log --no-edit master
git checkout master
