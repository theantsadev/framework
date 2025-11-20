## RESUME (alohan hiala ramose)
- 3 ter: determination url misy `{}` 
	- tokony capable mamaly `etudiant/{id}` 
	- tsy mbola misy mandefa donnees/variable
	- na inona na inona valeur an {id} dia le method `get(int id)` no maka azy
	- dynamique le valeur, fa atao null aloha
```java
	class EtudiantController {
		@url("/etudiant/{id}")
		get(int id) {
			/*
				id == null
				normal zany hatreto
				satria atao ze tsy amoahan le framework exception
				rehefa misy {}
			*/
		}
	}
```