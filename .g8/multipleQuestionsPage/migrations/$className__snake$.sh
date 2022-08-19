#!/bin/bash

echo ""
echo "Applying migration $className;format="snake"$"

echo "Adding routes to conf/app.routes"

echo "" >> ../conf/app.routes
echo "GET        /$className;format="decap"$                        uk.gov.hmrc.economiccrimelevyreturns.controllers.$className$Controller.onPageLoad(mode: Mode = NormalMode)" >> ../conf/app.routes
echo "POST       /$className;format="decap"$                        uk.gov.hmrc.economiccrimelevyreturns.controllers.$className$Controller.onSubmit(mode: Mode = NormalMode)" >> ../conf/app.routes

echo "GET        /change$className$                                 uk.gov.hmrc.economiccrimelevyreturns.controllers.$className$Controller.onPageLoad(mode: Mode = CheckMode)" >> ../conf/app.routes
echo "POST       /change$className$                                 uk.gov.hmrc.economiccrimelevyreturns.controllers.$className$Controller.onSubmit(mode: Mode = CheckMode)" >> ../conf/app.routes

echo "Adding messages to conf.messages"
echo "" >> ../conf/messages.en
echo "$className;format="decap"$.title = $className;format="decap"$" >> ../conf/messages.en
echo "$className;format="decap"$.heading = $className;format="decap"$" >> ../conf/messages.en
echo "$className;format="decap"$.$field1Name$ = $field1Name$" >> ../conf/messages.en
echo "$className;format="decap"$.$field2Name$ = $field2Name$" >> ../conf/messages.en
echo "$className;format="decap"$.checkYourAnswersLabel = $className$" >> ../conf/messages.en
echo "$className;format="decap"$.error.$field1Name$.required = Enter $field1Name$" >> ../conf/messages.en
echo "$className;format="decap"$.error.$field2Name$.required = Enter $field2Name$" >> ../conf/messages.en
echo "$className;format="decap"$.error.$field1Name$.length = $field1Name$ must be $field1MaxLength$ characters or less" >> ../conf/messages.en
echo "$className;format="decap"$.error.$field2Name$.length = $field2Name$ must be $field2MaxLength$ characters or less" >> ../conf/messages.en
echo "$className;format="decap"$.$field1Name$.change.hidden = $field1Name$" >> ../conf/messages.en
echo "$className;format="decap"$.$field2Name$.change.hidden = $field2Name$" >> ../conf/messages.en

echo "Migration $className;format="snake"$ completed"
