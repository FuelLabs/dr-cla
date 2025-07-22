/*
 * Copyright (c) 2018, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE.txt file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */

package modules

import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.regions.Region
import javax.inject.{Inject, Singleton}
import javax.inject.{Inject, Singleton}
import org.slf4j.LoggerFactory
import play.api.inject.{ApplicationLifecycle, Binding, Module}
import play.api.{Configuration, Environment}

import scala.concurrent.ExecutionContext

class DatabaseModule extends Module {
  def bindings(environment: Environment, configuration: Configuration): Seq[Binding[_]] = {
    Seq(
      bind[Database].to[DatabaseImpl]
    )
  }
}

trait Database {
  val dynamoDb: DynamoDbClient
}


@Singleton
class DatabaseImpl @Inject()(lifecycle: ApplicationLifecycle, playConfig: Configuration) extends Database {
  private val log = LoggerFactory.getLogger(this.getClass)
  private val region = playConfig.getOptional[String]("aws.dynamodb.region").getOrElse("us-east-1")
  //private val endpoint = playConfig.getOptional[String]("aws.dynamodb.endpoint")

  val dynamoDb: DynamoDbClient = {
    val builder = DynamoDbClient.builder().region(Region.of(region))
    //endpoint.foreach(e => builder.endpointOverride(java.net.URI.create(e)))
    builder.build()
  }
  lifecycle.addStopHook(() => {
    dynamoDb.close()
    scala.concurrent.Future.successful(())
  })
}
