package com.movieflix.movieApi.service;

import com.movieflix.movieApi.dto.MovieDto;
import com.movieflix.movieApi.dto.MoviePageResponse;
import com.movieflix.movieApi.entities.Movie;
import com.movieflix.movieApi.exceptions.FileExistsException;
import com.movieflix.movieApi.exceptions.MovieNotFoundException;
import com.movieflix.movieApi.repositories.MovieRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Service
public class MovieServiceImpl implements MovieService{

    private final MovieRepository movieRepository;

    private final FileService fileService;


    public MovieServiceImpl(MovieRepository movieRepository, FileService fileService) {
        this.movieRepository = movieRepository;
        this.fileService = fileService;
    }

    @Value("${project.poster}")
    private String path;

    @Value("${base.url}")
    private String baseUrl;

    @Override
    public MovieDto addMovie(MovieDto movieDto, MultipartFile file) throws IOException {

        if(Files.exists(Paths.get(path + File.separator+file.getOriginalFilename()))){
            throw new FileExistsException("File Already Exists! Please enter another file name");
        }
        //First upload the file, so that we can get a filename
        String uploadedFileName=fileService.uploadFile(path,file);

        //Now set the value of field poster as filename
        movieDto.setPoster(uploadedFileName);

        //Map MovieDto to Movie object since our db only accepts Movie object
        Movie movie=new Movie(
                null,
                movieDto.getTitle(),
                movieDto.getDirector(),
                movieDto.getStudio(),
                movieDto.getMovieCast(),
                movieDto.getReleaseYear(),
                movieDto.getPoster()
        );

        //Save the Movie object
        Movie savedMovie=movieRepository.save(movie);

        //Generate the posterurl and set it
        String posterUrl= baseUrl +"/file/"+uploadedFileName;

        //Map  and return this movieDTO
        MovieDto response =new MovieDto(
                savedMovie.getMovieId(),
                savedMovie.getTitle(),
                savedMovie.getDirector(),
                savedMovie.getStudio(),
                savedMovie.getMovieCast(),
                savedMovie.getReleaseYear(),
                savedMovie.getPoster(),
                posterUrl
        );
        return response;
    }

    @Override
    public MovieDto getMovie(Integer movieId) {

        //1. Check the data in Db if it exists and fetch the data of given ID
        Movie movie= movieRepository.findById(movieId).orElseThrow(()->new MovieNotFoundException("Movie Not Found with id = "+movieId));

        //2. Generate Posterurl
        String posterUrl= baseUrl +"/file/"+movie.getPoster();

        //3. Map to movie DTO obejct and return
        MovieDto response=new MovieDto(
                movie.getMovieId(),
                movie.getTitle(),
                movie.getDirector(),
                movie.getStudio(),
                movie.getMovieCast(),
                movie.getReleaseYear(),
                movie.getPoster(),
                posterUrl
        );
        return response;
    }

    @Override
    public List<MovieDto> getAllMovies() {
        //1. Fetch all the data from db
        List<Movie> movies=movieRepository.findAll();
        //2. Iterate throught the list and generate poster url for each movie object and map to movie Dto object
        List<MovieDto> movieDtos=new ArrayList<>();

        for(Movie movie: movies){
            String posterUrl= baseUrl +"/file/"+movie.getPoster();
            MovieDto response=new MovieDto(
                    movie.getMovieId(),
                    movie.getTitle(),
                    movie.getDirector(),
                    movie.getStudio(),
                    movie.getMovieCast(),
                    movie.getReleaseYear(),
                    movie.getPoster(),
                    posterUrl
            );
            movieDtos.add(response);
        }
        return movieDtos;
    }

    @Override
    public MovieDto updateMovie(Integer movieId, MovieDto movieDto, MultipartFile file) throws IOException {
        //1. First Check if movie exists with given id
        Movie mv= movieRepository.findById(movieId).orElseThrow(()->new MovieNotFoundException("Movie Not Found with id= "+movieId));

        //2. If file is null, then no need to update file associted with movieId else delete it and upload new file
        String fileName=mv.getPoster();
        if(file!=null){
            //Delete exisiting file
            Files.deleteIfExists(Paths.get(path+ File.separator+fileName));
            fileName=fileService.uploadFile(path,file);
        }

        //3. Set Movie Dto postervalue according to step 2
        movieDto.setPoster(fileName);

        //4. Map to movie Object
        Movie movie=new Movie(
                movieDto.getMovieId(),
                movieDto.getTitle(),
                movieDto.getDirector(),
                movieDto.getStudio(),
                movieDto.getMovieCast(),
                movieDto.getReleaseYear(),
                movieDto.getPoster()
        );
        //5.Save the movie object to repo
        movieRepository.save(movie);
        //6. Generate posterurl and finally map it to movieDto and return it
        String posterUrl= baseUrl +"/file/"+fileName;

        MovieDto response=new MovieDto(
                movie.getMovieId(),
                movie.getTitle(),
                movie.getDirector(),
                movie.getStudio(),
                movie.getMovieCast(),
                movie.getReleaseYear(),
                movie.getPoster(),
                posterUrl
        );
        return response;
    }

    @Override
    public String deleteMovie(Integer movieId) throws IOException {
       //1. Check if MovieObject exists in Db
        Movie mv=movieRepository.findById(movieId).orElseThrow(()->new MovieNotFoundException("Movie Not Found"));
        Integer id=mv.getMovieId();
        //2. Delete the file corresponding to movieObject
        Files.deleteIfExists(Paths.get(path+File.separator+mv.getPoster()));

        //3.Delete movieObject from repo
        movieRepository.delete(mv);

        return "Movie Deleted with id = "+id;
    }

    @Override
    public MoviePageResponse getAllMoviesWithPagination(Integer pageNumber, Integer pageSize) {
        Pageable pageable= PageRequest.of(pageNumber,pageSize);
        Page<Movie>moviePages=movieRepository.findAll(pageable);
        List<Movie>movies=moviePages.getContent();

        List<MovieDto> movieDtos=new ArrayList<>();

        for(Movie movie: movies){
            String posterUrl= baseUrl +"/file/"+movie.getPoster();
            MovieDto response=new MovieDto(
                    movie.getMovieId(),
                    movie.getTitle(),
                    movie.getDirector(),
                    movie.getStudio(),
                    movie.getMovieCast(),
                    movie.getReleaseYear(),
                    movie.getPoster(),
                    posterUrl
            );
            movieDtos.add(response);
        }
        return new MoviePageResponse(movieDtos, pageNumber, pageSize,
                moviePages.getTotalElements(),
                moviePages.getTotalPages(),
                moviePages.isLast());
    }

    @Override
    public MoviePageResponse getAllMoviesWithPagination(Integer pageNumber, Integer pageSize, String sortBy, String dir) {
        Sort sort=dir.equalsIgnoreCase("asc")?Sort.by(sortBy).ascending():Sort.by(sortBy).descending();

        Pageable pageable= PageRequest.of(pageNumber,pageSize,sort);
        Page<Movie>moviePages=movieRepository.findAll(pageable);
        List<Movie>movies=moviePages.getContent();

        List<MovieDto> movieDtos=new ArrayList<>();

        for(Movie movie: movies){
            String posterUrl= baseUrl +"/file/"+movie.getPoster();
            MovieDto response=new MovieDto(
                    movie.getMovieId(),
                    movie.getTitle(),
                    movie.getDirector(),
                    movie.getStudio(),
                    movie.getMovieCast(),
                    movie.getReleaseYear(),
                    movie.getPoster(),
                    posterUrl
            );
            movieDtos.add(response);
        }
        return new MoviePageResponse(movieDtos, pageNumber, pageSize,
                moviePages.getTotalElements(),
                moviePages.getTotalPages(),
                moviePages.isLast());
    }
}
