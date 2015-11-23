#!/bin/sh

cd $(dirname $0)/../web

SDK="../../../SDK\ext-premium-6.0.1\ext-6.0.1-trial\ext-6.0.1"

sencha compile --classpath=app.js,app,$SDK/packages/core/src,$SDK/packages/core/overrides,$SDK/classic/classic/src,$SDK/classic/classic/overrides \
       exclude -all \
       and \
       include -recursive -file app.js \
       and \
       exclude -namespace=Ext \
       and \
       concatenate -closure app.min.js
