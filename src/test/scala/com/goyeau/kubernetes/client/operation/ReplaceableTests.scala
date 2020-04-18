package com.goyeau.kubernetes.client.operation

import cats.Applicative
import cats.implicits._
import com.goyeau.kubernetes.client.KubernetesClient
import io.k8s.apimachinery.pkg.apis.meta.v1.ObjectMeta
import org.http4s.Status
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.{Assertion, OptionValues}
import org.scalatest.matchers.should.Matchers

trait ReplaceableTests[F[_], Resource <: { def metadata: Option[ObjectMeta] }]
    extends AnyFlatSpec
    with Matchers
    with OptionValues
    with MinikubeClientProvider[F] {

  def namespacedApi(namespaceName: String)(implicit client: KubernetesClient[F]): Replaceable[F, Resource]
  def createChecked(namespaceName: String, resourceName: String)(implicit client: KubernetesClient[F]): F[Resource]
  def getChecked(namespaceName: String, resourceName: String)(implicit client: KubernetesClient[F]): F[Resource]
  def sampleResource(resourceName: String): Resource
  def modifyResource(resource: Resource): Resource
  def checkUpdated(updatedResource: Resource): Assertion

  "replace" should s"replace a $resourceName" in usingMinikube { implicit client =>
    for {
      namespaceName <- Applicative[F].pure(resourceName.toLowerCase)
      resourceName  <- Applicative[F].pure("some-resource")
      resource      <- createChecked(namespaceName, resourceName)

      status <- namespacedApi(namespaceName).replace(modifyResource(resource))
      _ = status shouldBe Status.Ok
      replacedConfigMap <- getChecked(namespaceName, resourceName)
      _ = checkUpdated(replacedConfigMap)
    } yield ()
  }

  it should "fail on non existing namespace" in usingMinikube { implicit client =>
    for {
      status <- namespacedApi("non-existing").replace(sampleResource("non-existing"))
      _ = status shouldBe Status.NotFound
    } yield ()
  }

  it should s"fail on non existing $resourceName" in usingMinikube { implicit client =>
    for {
      namespaceName <- Applicative[F].pure(resourceName.toLowerCase)
      status        <- namespacedApi(namespaceName).replace(sampleResource("non-existing"))
      _ = status shouldBe Status.NotFound
    } yield ()
  }
}