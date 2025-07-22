/*
 * Copyright (c) 2018, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE.txt file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */

package utils

import javax.inject.Inject
import models.{ClaSignature, Contact}
import modules.Database
import software.amazon.awssdk.services.dynamodb.model._
import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}
import play.api.Configuration


class DB @Inject()(database: Database, config: Configuration)(implicit ec: ExecutionContext) {
  private val dynamoDb = database.dynamoDb
  private val tableName = config.get[String]("aws.dynamodb.table")

  def findContactByGitHubId(gitHubId: String): Future[Option[Contact]] = Future {
    val request = GetItemRequest.builder()
      .tableName(tableName)
      .key(Map("PK" -> AttributeValue.builder().s(s"CONTACT#$gitHubId").build()).asJava)
      .build()
    val result = dynamoDb.getItem(request)
    if (result.hasItem) {
      val item = result.item().asScala
      Some(Contact(
        id = item.getOrElse("id", AttributeValue.builder().s("").build()).s(),
        firstName = Some(item.getOrElse("firstName", AttributeValue.builder().s("").build()).s()),
        lastName = item.getOrElse("lastName", AttributeValue.builder().s("").build()).s(),
        email = item.getOrElse("email", AttributeValue.builder().s("").build()).s(),
        gitHubId = gitHubId
      ))
    } else None
  }

  def createContact(contact: Contact): Future[Contact] = Future {
    val item = Map(
      "PK" -> AttributeValue.builder().s(s"CONTACT#${contact.gitHubId}").build(),
      "id" -> AttributeValue.builder().s(contact.id).build(),
      "firstName" -> AttributeValue.builder().s(contact.firstName.getOrElse("")).build(),
      "lastName" -> AttributeValue.builder().s(contact.lastName).build(),
      "email" -> AttributeValue.builder().s(contact.email).build()
    ).asJava
    val request = PutItemRequest.builder().tableName(tableName).item(item).build()
    dynamoDb.putItem(request)
    contact
  }

  def createClaSignature(claSignature: ClaSignature): Future[ClaSignature] = Future {
    val item = Map(
      "PK" -> AttributeValue.builder().s(s"CLASIGNATURE#${claSignature.contactGitHubId}").build(),
      "id" -> AttributeValue.builder().s(claSignature.id).build(),
      "signedOn" -> AttributeValue.builder().s(claSignature.signedOn).build(),
      "claVersion" -> AttributeValue.builder().s(claSignature.claVersion).build(),
      "contactGitHubId" -> AttributeValue.builder().s(claSignature.contactGitHubId).build()
    ).asJava
    val request = PutItemRequest.builder().tableName(tableName).item(item).build()
    dynamoDb.putItem(request)
    claSignature
  }

  def findClaSignaturesByGitHubIds(gitHubIds: Set[GitHub.User]): Future[Set[ClaSignature]] = Future {
    gitHubIds.map { user =>
      val request = GetItemRequest.builder()
        .tableName(tableName)
        .key(Map("PK" -> AttributeValue.builder().s(s"CLASIGNATURE#${user.username}").build()).asJava)
        .build()
      val result = dynamoDb.getItem(request)
      if (result.hasItem) {
        val item = result.item().asScala
        Some(ClaSignature(
          id = item.getOrElse("id", AttributeValue.builder().s("").build()).s(),
          signedOn = item.getOrElse("signedOn", AttributeValue.builder().s("").build()).s(),
          claVersion = item.getOrElse("claVersion", AttributeValue.builder().s("").build()).s(),
          contactGitHubId = item.getOrElse("contactGitHubId", AttributeValue.builder().s("").build()).s()
        ))
      } else None
    }.flatten.toSet
  }

}
