# Instructions for Setting up the project

1. Run:
   ``brew install chromedriver``
2. Run:
   ``brew install python3``
3. Run:
   ``sudo pip3 install -U pipenv``
4. In the root directory, run:
   ``pipenv sync``
5. move the folder to '~/vizmod'
5. In the root directory, run:  
   ``cp matplotlibrc ~/.matplotlib/matplotlibrc``
6. In the root directory, run:  
	``pipenv shell``
	``sudo python3 src/python/main/refactor.py <URL>``
