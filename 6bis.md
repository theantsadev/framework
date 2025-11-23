- 6 bis
	- raha misy `@RequestParam`
	- raha argument ka misy @RequestParam(int id)
		- verifiena raha misy an le variable anaty getParam 
		- na tsisy var 1 ary anaty getParam
		- -> le annotation no mitondra fa tsy le var 1
```java
class EtudiantController {
	@url("/etudiant/{id}")
	get(@RequestParam(int id) var1, String var2, int id) {
		// var1 disponible 
		
		/*
			ilay manao conversion (Date var3) afaka mampiasa librairie
		*/
		
		/*
			var 1 = id 
			satria int id aloha no asiany value 
			zay vao le var1
		*/
	}
}
```