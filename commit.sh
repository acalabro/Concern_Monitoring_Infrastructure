#!/bin/bash
git filter-branch --env-filter '
if [ "$GIT_COMMITTER_EMAIL" = "acalabro@gatto" ]; then
    export GIT_COMMITTER_EMAIL="antonello.calabro@gmail.com"
fi
if [ "$GIT_AUTHOR_EMAIL" = "acalabro@gatto" ]; then
    export GIT_AUTHOR_EMAIL="antonello.calabro@gmail.com"
fi
' --tag-name-filter cat -- --branches --tags
