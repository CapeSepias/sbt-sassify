/*
 * Copyright 2015 Han van Venrooij
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.irundaia.sbt.sass

import java.io.File

import org.irundaia.sbt.sass.compiler.{SassCompilerException, SassCompiler, CompilerSettings}
import org.scalatest.{FunSpec, MustMatchers}

import scala.io.Source
import scala.util.Try

class SassCompilerTest extends FunSpec with MustMatchers {

  val compilerSettings = CompilerSettings(Minified, true, true, Auto)

  describe("The SassCompiler") {
    describe("using well formed scss input") {
      describe("without includes") {
        val input = new File(getClass.getResource("/org/irundaia/sbt/sass/well-formed.scss").toURI)
        val output = File.createTempFile("sbt-sass-test", ".css")
        val map = File.createTempFile("sbt-sass-test", ".css.map.json")

        val compilationResults = Try(new SassCompiler(compilerSettings)
          .doCompile(input, output, map, "", ""))
        val cssMin = Source.fromFile(output).mkString

        val testMinCss = cssMin.replaceAll("\\/\\*.*?\\*\\/", "").replaceAll("\\s+", "")

        it("should compile") {
          compilationResults.isSuccess mustBe true
        }

        it("should contain the proper contents") {
          testMinCss must include(".test{font-size:10px")
          testMinCss must include(".test.hidden{display:none")
        }

        it("should have read one file") {
          compilationResults.get.filesRead.size must be(1)
        }

        it("should have read the correct file") {
          compilationResults.get.filesRead.head.getCanonicalPath must include("well-formed.scss")
        }
      }

      describe("with includes") {
        val input = new File(getClass.getResource("/org/irundaia/sbt/sass/well-formed-using-import.scss").toURI)
        val output = File.createTempFile("sbt-sass-test", ".css")
        val map = File.createTempFile("sbt-sass-test", ".css.map.json")

        val compilationResult = Try(new SassCompiler(compilerSettings)
          .doCompile(input,  output, map, "", ""))

        val cssMin = Source.fromFile(output).mkString
        val testMinCss = cssMin.replaceAll("\\/\\*.*?\\*\\/", "").replaceAll("\\s+", "")

        it("should compile") {
          compilationResult.isSuccess mustBe true
        }

        it("should include the contents of both the included and the including file") {
          testMinCss must include(".test-import{font-weight:bold")
          testMinCss must include(".test{font-size:10px")
          testMinCss must include(".test.hidden{display:none")
        }

        it("should have read two files") {
          compilationResult.get.filesRead.size must be(2)
        }

        it("should have read the included file") {
          compilationResult.get.filesRead.map(_.getCanonicalPath).find(_.contains("_well-formed-import.scss")) must not be None
        }
      }
    }

    describe("using broken scss input") {
      it("should throw an exception") {
        val input = new File(getClass.getResource("/org/irundaia/sbt/sass/broken-input.scss").toURI)
        val output = File.createTempFile("sbt-sass-test", ".css")
        val map = File.createTempFile("sbt-sass-test", ".css.map.json")

        val exception = the[SassCompilerException] thrownBy
          new SassCompiler(compilerSettings)
            .doCompile(input, output, map, "", "")

        exception.problem.message mustEqual "invalid property name"
      }
    }
  }
}