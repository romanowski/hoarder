## Storing artifacts in amazon s3

In many situations we need to carry caches across different machine (e.g. from IC server to local machine or within machines inside CI plant). Amazon S3 provides convenient place to store them.

## Installation

Steps to enable s3 caches in your project:

 - add hoarder-amazon plugin to your build by adding following line to your `plugins.sbt` file (or any .sbt file in project directory): `addSbtPlugin("com.github.romanowski" %% "hoarder-amazon" % "1.2.0")`
 - create bucket (e.g. `hoarder-test`) and create user that has permission to read and write to that bucket (it should has `Programmatic access` type and remember to note down access key ID and secret access key). Example policy attached to user:
 ```json
  {
      "Version": "2012-10-17",
      "Statement": [
          {
              "Sid": "",
              "Effect": "Allow",
              "Action": [
                  "s3:*"
              ],
              "Resource": [
                  "arn:aws:s3:::hoarder-test*"
              ]
          }
      ]
  }
 ```
 - Configure bucket information in your plugin. To do so we create file named `HoarderS3Config.scala` in `project` directory with following content:
 ```scala
import org.romanowski.hoarder.amazon.HoarderS3 

object HoarderS3Test extends HoarderS3.Simple("hoarder-test")
 ```
 - Make sure that you can access created bucket from command line without providing login/password (e.g. by having AWS_ACCESS_KEY_ID and AWS_SECRET_ACCESS_KEY local variables in scope)
 - Now you can enjoy `loadCache` and `storeCache` tasks that work similar to `stash` and `stashApply` tasks from [Stash workflow](stash.md) 