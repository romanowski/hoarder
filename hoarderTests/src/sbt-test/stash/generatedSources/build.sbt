sourceGenerators in Compile += Def.task[Seq[File]] {
	val file = sourceManaged.value / "Gen.scala"
	IO.write(file, "class Gen")
	file :: Nil
}

sourceGenerators in Test += Def.task[Seq[File]] {
	val file = sourceManaged.value / "GenTest.scala"
	IO.write(file, "class GenTest")
	file :: Nil
}